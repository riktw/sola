package sola

import spinal.core._

case class SumpInterface(gpioWidth : Int) extends Bundle {
  val triggerMask = Bits(gpioWidth bits)
  val triggerValue = Bits(gpioWidth bits)
  val triggerState = Bool
  val start = Bool
  val arm = Bool
  val divider = UInt(24 bits)
  val readCount = UInt(16 bits)
  val delayCount = UInt(16 bits)
}

object SUMPProtocol {

  def SUMP_RESET = B"8'x00"
  def SUMP_ARM = B"8'x01"
  def SUMP_QUERY = B"8'x02"
  def SUMP_TEST = B"8'x03"
  def SUMP_GET_SUMP_METADATA = B"8'x04"
  def SUMP_RLE_FINISH = B"8'x05"
  def SUMP_XON = B"8'x11"
  def SUMP_XOFF = B"8'x13"
  def SUMP_SET_TRIGGER_MASK = B"8'xC0"
  def SUMP_SET_TRIGGER_VALUES = B"8'xC1"
  def SUMP_SET_TRIGGER_CONF = B"8'xC2"
  def SUMP_SET_DIVIDER = B"8'x80"
  def SUMP_SET_READ_DELAY_COUNT = B"8'x81"
  def SUMP_SET_FLAGS = B"8'x82"

  def SUMP_ID_1 = B"8'x31"
  def SUMP_ID_2 = B"8'x41"
  def SUMP_ID_3 = B"8'x4C"
  def SUMP_ID_4 = B"8'x53"

  def SUMP_ID = Vec(SUMP_ID_1, SUMP_ID_2, SUMP_ID_3, SUMP_ID_4)

}
