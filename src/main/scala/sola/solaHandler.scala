package sola

import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._
import spinal.lib.fsm._
import sola.SUMPProtocol._

class solaHandler(gpioWidth : Int, bufferSize : Int, uartDiv : Int, fakeClock : Boolean, smallConfig : Boolean) extends Component {
  val io = new Bundle {
    val uart = master(Uart())
    val cancelSampling = out Bool
    val SamplingParameters = out(SumpInterface(gpioWidth*32))
    val address = master(Stream(UInt((log2Up(bufferSize) + 1) bits)))
    val data = slave(Stream(Bits(gpioWidth*32 bits)))
    val dataReady = in Bool
  }

  val triggerMask = Reg(Bits(gpioWidth*32 bits)) init(0)
  val triggerValue = Reg(Bits(gpioWidth*32 bits)) init(0)
  val triggerState = Reg(Bool) init(False)
  val start = Reg(Bool()) init(False)
  val arm = Reg(Bool()) init(False)
  val divider = Reg(UInt(24 bits)) init(0)
  val readCount = Reg(UInt(16 bits)) init(0)
  val delayCount = Reg(UInt(16 bits)) init(0)
  val addressPayload = Reg(UInt((log2Up(bufferSize) + 1) bits)) init(0)
  val addressValid = Reg(Bool) init(False)
  val dataReady = Reg(Bool) init(False)
  val channelGroups = Reg(Bits(gpioWidth*4 bits)) init(0)

  val fakeClockReg = Reg(Bool) init(False)

  val memorySizeInBytes = IntToBits(bufferSize*4)
  def metaData = List(
    B"8'x01",  B"8'x73",  B"8'x6F",  B"8'x6C",  B"8'x61", B"8'x00",     //Name
    B"8'x21",  B"8'x00",  B"8'x00",  IntToBits((bufferSize*gpioWidth)>>6),  B"8'x00",               //Samples
    B"8'x23",  B"8'x00",  B"8'x5B",  B"8'x8D",  B"8'x80",               //Max speed
    B"8'x40",  IntToBits(gpioWidth*32),                                 //Channels
    B"8'x41",  B"8'x02",                                                //Version
    B"8'x00"
  )

  val metaDataBuffer = Mem(Bits(8 bits), metaData)

  io.address.payload := addressPayload
  io.address.valid := addressValid
  io.data.ready := dataReady
  io.SamplingParameters.triggerMask := triggerMask
  io.SamplingParameters.triggerValue := triggerValue
  io.SamplingParameters.triggerState := triggerState
  io.SamplingParameters.start := start
  io.SamplingParameters.arm := arm
  io.SamplingParameters.divider := divider
  io.SamplingParameters.readCount := readCount
  io.SamplingParameters.delayCount := delayCount
  io.cancelSampling := False

  val uartCtrlConfig = UartCtrlGenerics(
    dataWidthMax = 8,
    clockDividerWidth = 8,
    preSamplingSize = 1,
    samplingSize = 5,
    postSamplingSize = 2)
  val uartCtrl = new UartCtrl(uartCtrlConfig)
  uartCtrl.io.config.clockDivider := uartDiv
  uartCtrl.io.config.frame.dataLength := 7  //8 bits
  uartCtrl.io.config.frame.parity := UartParityType.NONE
  uartCtrl.io.config.frame.stop := UartStopType.ONE
  uartCtrl.io.uart <> io.uart
  val uartWriteValid = Reg(Bool) init(False)
  val uartWriteData = Reg(Bits(8 bits)) init(0)
  uartCtrl.io.write.valid := uartWriteValid
  uartCtrl.io.writeBreak := False;
  uartCtrl.io.write.payload := uartWriteData

  val fsm : StateMachine = new StateMachine {
    val commandByte = Reg(Bits(8 bits)) init(0)
    val bytesToRead = Reg(UInt(log2Up(1+gpioWidth*4) bits)) init(0)
    val bytesRead = Reg(Vec(Bits(8 bits), (gpioWidth*4)))
    val bytesToWrite = Reg(UInt(5 bits)) init(0)
    val writeMeta = Reg(Bool) init(false)

    val dataRead = Reg(Bits(gpioWidth*32 bits)) init (0)
    val byteCounter = Counter(0 until gpioWidth * 4)
    val dataInFlight = Reg(Bool) init (False)

    val stateIdle : State = new State with EntryPoint {
      whenIsActive {

        when(uartCtrl.io.write.ready) {
          uartWriteValid := False
        }

        when(uartCtrl.io.read.valid === True) {
          val byteReceived = uartCtrl.io.read.payload
          commandByte := byteReceived;
          when(byteReceived === SUMP_RESET) {

          }.elsewhen(byteReceived === SUMP_ARM) {
            arm := True
            goto(waitingForSampler)
          }.elsewhen(byteReceived === SUMP_QUERY) {
            bytesToWrite := 4
            uartWriteValid := True
            uartWriteData := SUMP_ID(0)
            writeMeta := False
            goto(stateWriteCommand)
          }.elsewhen(byteReceived === SUMP_GET_SUMP_METADATA) {
            bytesToWrite := metaData.size - 1
            uartWriteValid := True
            uartWriteData := metaDataBuffer.readSync(U"00000")
            writeMeta := True
            goto(stateWriteCommand)
          }.elsewhen(byteReceived === SUMP_XON) {
            start := True
          }.elsewhen(byteReceived === SUMP_XOFF) {
            start := False
          }.otherwise {
            bytesToRead := gpioWidth*4;
            goto(stateReadCommand)
          }
        }
      }
    }

    val waitingForSampler : State = new State {
      whenIsActive {
        arm := False
        when(uartCtrl.io.read.valid === True) {
          when(uartCtrl.io.read.payload === SUMP_RESET) {
            io.cancelSampling := True
            goto(stateIdle)
          }
        }
        when(io.dataReady) {
          addressPayload := (readCount - 1).resized
          dataInFlight := False
          goto(stateSendPayload)
        }
      }
    }

    val stateReadCommand : State = new State {
      whenIsActive {
        when(uartCtrl.io.read.valid === True) {
          bytesRead((bytesToRead - 1).resized) := uartCtrl.io.read.payload
          bytesToRead := bytesToRead - 1
        }
        when(bytesToRead === 0) {
          val bytesReadEnd = Vec(Bits(8 bits), (gpioWidth*4))
          //endianness swap per 32 bits
          for(x <- 0 until gpioWidth) {
            bytesReadEnd(x*4 + 0) := bytesRead(x*4 + 3)
            bytesReadEnd(x*4 + 1) := bytesRead(x*4 + 2)
            bytesReadEnd(x*4 + 2) := bytesRead(x*4 + 1)
            bytesReadEnd(x*4 + 3) := bytesRead(x*4 + 0)
          }

          when(commandByte === SUMP_SET_DIVIDER) {
            if(!smallConfig) {
              divider := (bytesRead.asBits.asUInt).resized
            }
          }.elsewhen(commandByte === SUMP_SET_READ_DELAY_COUNT){
            if(fakeClock) {
              readCount := (2 * (1 + bytesReadEnd.asBits(15 downto 0).asUInt)).resized
              delayCount := (2 * (1 + bytesReadEnd.asBits(31 downto 16).asUInt)).resized
            }
            else {
              readCount := (4 * (1 + bytesReadEnd.asBits(15 downto 0).asUInt)).resized
              delayCount := (4 * (1 + bytesReadEnd.asBits(31 downto 16).asUInt)).resized
            }
          }.elsewhen(commandByte === SUMP_SET_FLAGS){
            if(!smallConfig) {
              for (x <- 1 to gpioWidth) {
                channelGroups((x * 4) - 1 downto (x * 4) - 4) := bytesReadEnd.asBits(((x - 1) * 32) + 5 downto ((x - 1) * 32) + 2)
              }
            }
          }.elsewhen(commandByte === SUMP_SET_TRIGGER_MASK){
            triggerMask := bytesReadEnd.asBits.resized
          }.elsewhen(commandByte === SUMP_SET_TRIGGER_VALUES){
            triggerValue := bytesReadEnd.asBits.resized
          }.elsewhen(commandByte === SUMP_SET_TRIGGER_CONF){
            triggerState := !bytesReadEnd.asBits((3*8)+3)
          }.otherwise {
            //Unused, ignore
          }

          goto(stateIdle)
        }
      }
    }

    val stateWriteCommand : State = new State {
      whenIsActive {
        when(bytesToWrite =/= 0){
          when(uartCtrl.io.write.ready === True) {
            uartWriteValid := True
            when(writeMeta) {
              uartWriteData := metaDataBuffer((metaData.size - bytesToWrite).resized)
            }.otherwise {
              uartWriteData := SUMP_ID((5 - bytesToWrite).resized)
            }
            bytesToWrite := bytesToWrite - 1;
          }
        }
          .otherwise{
            uartWriteValid := False
            uartWriteData := B"x00"
            goto(stateIdle)
          }
      }
    }

    val stateSendPayload : State = new StateFsm (
      new StateMachine {
        val stateFetchNewData: State = new State with EntryPoint {
          whenIsActive {
            when(addressPayload === (IntToUInt((bufferSize*2)-1))) {
              exit()
            }.otherwise {
              when(io.address.ready) {
                addressValid := True
              }
              when(io.address.fire) {
                addressValid := False
                dataReady := True
              }
              when(io.data.valid) {
                dataReady := False
                dataRead := io.data.payload
                dataRead(0) := True
                goto(stateGetNextByteToTransmit)
              }
            }
          }
        }
        val stateGetNextByteToTransmit: State = new State {
          whenIsActive {
            if(smallConfig) {
              goto(stateTransmitData)
            }
            else {
              when(!channelGroups(byteCounter.value)) {
                goto(stateTransmitData)
              }.otherwise {
                byteCounter.increment
                when(byteCounter.valueNext === 0) {
                  addressPayload := addressPayload - 1
                  goto(stateFetchNewData)
                }
              }
            }
          }
        }
        val stateTransmitData: State = new State {
          whenIsActive {
            when(!dataInFlight) {
              dataInFlight := True
              uartWriteValid := True
              uartWriteData := dataRead.subdivideIn(8 bits)(byteCounter.value)
              byteCounter.increment
              goto(stateGetNextByteToTransmit)
            }.otherwise {
              when(uartCtrl.io.write.ready) {
                uartWriteValid := True
                uartWriteData := dataRead.subdivideIn(8 bits)(byteCounter.value)
                byteCounter.increment
                when(byteCounter.valueNext === 0) {
                  if(fakeClock) {
                    when(fakeClockReg) {
                      fakeClockReg := False
                      addressPayload := addressPayload - 1
                      goto(stateFetchNewData)
                    }.otherwise {
                      fakeClockReg := True
                      goto(stateGetNextByteToTransmit)
                      dataRead(0) := False
                    }
                  }
                  else
                  {
                    addressPayload := addressPayload - 1
                    goto(stateFetchNewData)
                  }
                }.otherwise {
                  goto(stateGetNextByteToTransmit)
                }
              }
            }
          }
        }
      }
    ) {
      whenCompleted(goto(stateIdle))
    }
  }
}

import spinal.core.sim._
import SpinalSimHelpers._
import SpinalSimHelpers.UartHelper._

//MyTopLevel's testbench
object solaHandlerSim {
  def main(args: Array[String]) {

    SimConfig.withWave.doSim(new solaHandler(1, 1024,1, false, false)){dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      dut.io.uart.rxd #= true
      dut.io.dataReady #= false

      dut.clockDomain.waitSampling(100)

      val baudPeriod = calculateBaudPeriod(115200, 1000)
      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
      dut.clockDomain.waitSampling(1000)

      transmitUart(0x81, baudPeriod, dut.io.uart.rxd)
      transmitUart(0x05, baudPeriod, dut.io.uart.rxd)
      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
      transmitUart(0x03, baudPeriod, dut.io.uart.rxd)
      transmitUart(0x04, baudPeriod, dut.io.uart.rxd)

      dut.clockDomain.waitSampling(10000)

      transmitUart(0x01, baudPeriod, dut.io.uart.rxd)

      dut.clockDomain.waitSampling(500000)
      dut.io.dataReady #= true
      dut.clockDomain.waitSampling(1)
      dut.io.dataReady #= false

      dut.clockDomain.waitSampling(500000)
    }
  }
}
