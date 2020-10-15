# sola
small open logic analyzer

Still in development!

### usage

To configure sola, 5 parameters exist:

 - channelWidth is the number of logic channels times 32. a channelWidth of 2 means 64 channels.

 - bufferSize is the number of bytes of sample memory. It must be 1024 of a multiple of 1024.

 - uartDiv is the uart divider. To calculate: (Clock speed / 8  / 115200) - 1. For example, with a 12Mhz clock: (12.000.000 / 8 / 115200) - 1 = 12.

 - fakeClock: For internal logic analyzer usage, a fake clock can be added to channel 0. This way, internal signals are sampled at the clock speed but a clock signal is still visible when analyzing.

  - smallConfig: This disables setting the samplerate lower then the clock and setting channel groups. Those are generally not needed for internal LA use and save some space.


#### using SpinalHDL
TODO

#### generating verilog 
Change the config at solaTopVerilog to match your wishes and run spinalHDL as described in their guide.
