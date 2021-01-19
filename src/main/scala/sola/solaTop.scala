package sola

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
import spinal.lib.com.uart._
import spinal.lib.com.uart.Uart

class solaTop(channelWidth : Int, bufferSize : Int, uartDiv : Int, fakeClock : Boolean, smallConfig : Boolean, speedDivider : Int) extends Component {
  val io = new Bundle {
    val uart = master(Uart())
    val statusLEDs = out Bits(3 bits)
    val dataPins = in Bits(channelWidth*32 bits)
  }

  val solaHandler = new solaHandler(channelWidth, bufferSize / (channelWidth*4), uartDiv, fakeClock, smallConfig, speedDivider)
  val Sampler = new Sampler(channelWidth, bufferSize / (channelWidth*4), smallConfig, speedDivider)
  
  io.statusLEDs := Sampler.io.statusLEDs
  io.uart <> solaHandler.io.uart
  
  Sampler.io.SamplingParameters := solaHandler.io.SamplingParameters
  Sampler.io.cancelSampling := solaHandler.io.cancelSampling
  Sampler.io.address <> solaHandler.io.address
  Sampler.io.dataPins := io.dataPins
  solaHandler.io.data <> Sampler.io.data
  solaHandler.io.dataReady := Sampler.io.dataReady
}

//Generate Verilog
object solaTopVerilog {
  def main(args: Array[String]) {
    SpinalVerilog(new solaTop(channelWidth = 4, bufferSize = 16384, uartDiv = 12, fakeClock = true, smallConfig = true, speedDivider = 10)) 
  }
}


//Generate VHDL
object solaTopVHDL {
  def main(args: Array[String]) {
    SpinalVhdl(new solaTop(channelWidth = 4, bufferSize = 16384, uartDiv = 12, fakeClock = true, smallConfig = true, speedDivider = 10)) 
  }
}


//import spinal.core.sim._
//import SpinalSimHelpers._
//import SpinalSimHelpers.UartHelper._

//object solaTopSim {
//  def main(args: Array[String]) {
//    /*
//    //100Mhz clock, 8 clocks per bit, 115.2K = 108, 107 as of offset
//    SimConfig.withWave.doSim(new solaTop(1, 2048,107)){dut =>
//      //Fork a process to generate the reset and the clock on the dut
//      dut.clockDomain.forkStimulus(period = 10)
//
//      dut.io.uart.rxd #= true
//      dut.io.dataPins #= 0x12345678
//
//      dut.clockDomain.waitSampling(100)
//
//      val baudPeriod = calculateBaudPeriod(115200, 1000)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      dut.clockDomain.waitSampling(1000)
//
//      transmitUart(0x80, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x63, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0x81, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x0A, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x0A, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0x82, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC0, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x07, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC1, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x07, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC2, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x08, baudPeriod, dut.io.uart.rxd)  //08 for trigger test
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0x01, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(2000000)
//
//    }
//    */
//
///*
//    SimConfig.withWave.doSim(new solaTop(2, 16384,107, true, true)){dut =>
//      //Fork a process to generate the reset and the clock on the dut
//      dut.clockDomain.forkStimulus(period = 10)
//
//      dut.io.uart.rxd #= true
//      //dut.io.dataPins #= 0x12345678
//
//      dut.clockDomain.waitSampling(100)
//
//      val baudPeriod = calculateBaudPeriod(115200, 1000)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      dut.clockDomain.waitSampling(1000)
//
//      transmitUart(0x80, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x63, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0x81, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x0A, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x0A, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0x82, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC0, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x01, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x01, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x80, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x07, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC1, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x07, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC2, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x08, baudPeriod, dut.io.uart.rxd)  //08 for trigger test
//
//
//      dut.clockDomain.waitSampling(20000)
//
//      transmitUart(0x01, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(3500000)
//
//    }
//
//    */
//
//    SimConfig.withWave.doSim(new solaTop(4, 16384,107, true, true)){dut =>
//      //Fork a process to generate the reset and the clock on the dut
//      dut.clockDomain.forkStimulus(period = 10)
//
//      dut.io.uart.rxd #= true
//      //dut.io.dataPins #= 0x12345678
//
//      dut.clockDomain.waitSampling(100)
//
//      val baudPeriod = calculateBaudPeriod(115200, 1000)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      dut.clockDomain.waitSampling(1000)
//
//      transmitUart(0x80, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x63, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0x81, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x0A, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x0A, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0x82, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x18, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC0, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x20, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x40, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x10, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC1, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x20, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(10000)
//
//      transmitUart(0xC2, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x20, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//      transmitUart(0x00, baudPeriod, dut.io.uart.rxd)
//
//
//      dut.clockDomain.waitSampling(20000)
//
//      transmitUart(0x01, baudPeriod, dut.io.uart.rxd)
//
//      dut.clockDomain.waitSampling(3500000)
//
//    }
//
//
//  }
//}
//
