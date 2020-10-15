package sola

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

class Sampler(channelWidth : Int, bufferSize : Int, smallConfig : Boolean) extends Component {
  val io = new Bundle {

    val dataPins = in Bits(channelWidth*32 bits)
    val SamplingParameters = in(SumpInterface(channelWidth*32))
    val cancelSampling = in Bool

    val statusLEDs = out Bits(3 bits)

    val address = slave(Stream(UInt((log2Up(bufferSize) + 1) bits)))
    val data = master(Stream(Bits(channelWidth*32 bits)))
    val dataReady = out Bool

  }

  val dataReady = Reg(Bool) init(False)
  val sampleCount = Reg(UInt((log2Up(bufferSize) + 1) bits)) init(0)
  val noTriggerSampleCount = Reg(UInt((log2Up(bufferSize) + 1) bits)) init(0)
  val addressRead = Reg(UInt((log2Up(bufferSize) + 1) bits)) init(0)
  val dataValid = Reg(Bool) init(False)

  val sampleBuffer = Mem(Bits(channelWidth*32 bits), wordCount = bufferSize)
  val sampleBufferAddress = UInt((log2Up(bufferSize) + 1) bits)
  val sampleBufferData = Bits(channelWidth*32 bits)
  val sampleBufferRW = Bool

  io.data.payload := sampleBuffer.readWriteSync(sampleBufferAddress.resized, sampleBufferData, True, sampleBufferRW)
  sampleBufferAddress := 0
  sampleBufferData := io.dataPins
  sampleBufferRW := False

  io.address.ready := True
  io.data.valid := dataValid
  io.dataReady := dataReady
  io.statusLEDs := "001"

  val fsm : StateMachine = new StateMachine {
    val dividerCount = Reg(UInt(24 bits)) init(0)

    val stateIdle : State = new State with EntryPoint {
      whenIsActive {
        dataReady := False

        when(io.SamplingParameters.arm) {
          noTriggerSampleCount := 0
          sampleCount := 0
          when(io.SamplingParameters.triggerState) {
            goto(stateCaptureNoTrigger)
          }.otherwise {
            goto(stateCaptureTriggered)
          }
        }

        when(io.address.valid) {
          addressRead := io.address.payload
        }
        when(io.data.ready) {
          dataValid := True
          val arrayStart = UInt(16 bits)
          when(io.SamplingParameters.readCount > io.SamplingParameters.delayCount) {
            arrayStart := io.SamplingParameters.readCount - io.SamplingParameters.delayCount //haha this will explode on a underflow
          }.otherwise {
            arrayStart := 0
          }
          sampleBufferAddress := ((noTriggerSampleCount - arrayStart) + addressRead).resized
        }

        when(io.data.fire) {
          dataValid := False
        }

      }
    }

    val stateCaptureNoTrigger : State = new State {
      onEntry(dividerCount := 0)
      whenIsActive {
        io.statusLEDs := "010"
        when(io.cancelSampling) {
          goto(stateIdle)
        }
        when((io.dataPins & io.SamplingParameters.triggerMask) === io.SamplingParameters.triggerValue) {
          goto(stateCaptureTriggered)
        }
        if(smallConfig) {
          sampleBufferAddress := noTriggerSampleCount.resized
          sampleBufferRW := True
        }
        else {
          when(dividerCount >= io.SamplingParameters.divider) {
            sampleBufferAddress := noTriggerSampleCount.resized
            sampleBufferRW := True
            dividerCount := 0
          }.otherwise {
            dividerCount := dividerCount + 10
          }
        }
        noTriggerSampleCount := noTriggerSampleCount + 1
      }
    }

    val stateCaptureTriggered: State = new State {
      onEntry(dividerCount := 0)
      whenIsActive {
        io.statusLEDs := "100"
        when(io.cancelSampling) {
          goto(stateIdle)
        }
        if(smallConfig) {
          sampleBufferAddress := (noTriggerSampleCount + sampleCount).resized
          sampleBufferRW := True
          sampleCount := sampleCount + 1
        }
        else {
          when(dividerCount >= io.SamplingParameters.divider) {
            sampleBufferAddress := (noTriggerSampleCount + sampleCount).resized
            sampleBufferRW := True
            sampleCount := sampleCount + 1
            dividerCount := 0
          }.otherwise {
            dividerCount := dividerCount + 10
          }
        }
        when(sampleCount === io.SamplingParameters.readCount) {
          dataReady := True
          goto(stateIdle)
        }
      }
    }
  }
}

import spinal.core.sim._

object SamplerSim {
  def main(args: Array[String]) {

    SimConfig.withWave.doSim(new Sampler(1,  1024, false)) { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      dut.io.dataPins #= 0x00000000
      dut.io.SamplingParameters.triggerMask #= 0x00000010
      dut.io.SamplingParameters.triggerValue #= 0x00000010
      dut.io.SamplingParameters.triggerState #= true
      dut.io.SamplingParameters.start #= false
      dut.io.SamplingParameters.arm #= false
      dut.io.SamplingParameters.divider #= 0x0000001E
      dut.io.SamplingParameters.readCount #= 0x0010
      dut.io.SamplingParameters.delayCount #= 0x0010

      dut.io.address.payload #= 0x00000000
      dut.io.address.valid #= false

      dut.clockDomain.waitSampling(10)

      //arm and start sampling
      dut.io.SamplingParameters.arm #= true
      dut.clockDomain.waitSampling(1)
      dut.io.SamplingParameters.arm #= false
      var counter = 0
      while(!dut.io.dataReady.toBoolean) {
        dut.clockDomain.waitSampling(1)
        counter = counter + 1
        dut.io.dataPins #= counter
      }

      dut.clockDomain.waitSampling(10)

      for(x <- 0 to dut.io.SamplingParameters.readCount.toInt) {
        dut.io.address.payload #= x
        dut.io.address.valid #= true
        dut.io.data.ready #= true
        dut.clockDomain.waitSampling(1)
      }

      dut.clockDomain.waitSampling(10)

    }
  }
}
