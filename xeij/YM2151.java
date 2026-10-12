//========================================================================================
//  YM2151.java
//    en:YM2151
//    ja:YM2151
//  Copyright (C) 2003-2022 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//--------------------------------------------------------------------------------
//  This program is based on ymfm developed by Aaron Giles.
//  https://github.com/aaronsgiles/ymfm
//--------------------------------------------------------------------------------

//--------------------------------------------------------------------------------
//ymfm License
//--------------------------------------------------------------------------------
// BSD 3-Clause License
//
// Copyright (c) 2021, Aaron Giles
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// 3. Neither the name of the copyright holder nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//--------------------------------------------------------------------------------

package xeij;

import java.io.*;  //FileOutputStream
import java.nio.*;  //ByteBuffer
import java.util.*;  //Arrays

public class YM2151 {



  public static int sint8 (int x) {
    return (byte) x;
  }
  public static int sint16 (int x) {
    return (short) x;
  }
  public static int uint8 (int x) {
    return 0xff & x;
  }
  public static int uint16 (int x) {
    return (char) x;
  }
  public static int umin32 (int x, int y) {
    return Integer.compareUnsigned (x, y) < 0 ? x : y;
  }



  //-------------------------------------------------
  //  bitfield - extract a bitfield from the given
  //  value, starting at bit 'start' for a length of
  //  'length' bits
  //-------------------------------------------------
  public static /*uint32*/ int bitfield (/*uint32*/ int value, int start) {
    return (value >>> start) & 1;
  }
  public static /*uint32*/ int bitfield (/*uint32*/ int value, int start, int length) {
    return (value >>> start) & ((1 << length) - 1);
  }

  //-------------------------------------------------
  //  clamp - clamp between the minimum and maximum
  //  values provided
  //-------------------------------------------------
  public static /*sint32*/ int clamp (/*sint32*/ int value, /*sint32*/ int minval, /*sint32*/ int maxval) {
    //return Math.max (minval, Math.min (maxval, value));
    if (value < minval) {
      return minval;
    }
    if (value > maxval) {
      return maxval;
    }
    return value;
  }

  //-------------------------------------------------
  //  count_leading_zeros - return the number of
  //  leading zeros in a 32-bit value; CPU-optimized
  //  versions for various architectures are included
  //  below
  //-------------------------------------------------
  public static /*uint8*/ int count_leading_zeros (/*uint32*/ int value) {
    //return uint8 (Integer.numberOfLeadingZeros (value));
    if (value == 0) {
      return 32;
    }
    /*uint8*/ int count;
    for (count = 0; /*sint32*/ value >= 0; count++) {
      value <<= 1;
    }
    return count;
  }

  // Many of the Yamaha FM chips emit a floating-point value, which is sent to
  // a DAC for processing. The exact format of this floating-point value is
  // documented below. This description only makes sense if the "internal"
  // format treats sign as 1=positive and 0=negative, so the helpers below
  // presume that.
  //
  // Internal OPx data      16-bit signed data     Exp Sign Mantissa
  // =================      =================      === ==== ========
  // 1 1xxxxxxxx------  ->  0 1xxxxxxxx------  ->  111   1  1xxxxxxx
  // 1 01xxxxxxxx-----  ->  0 01xxxxxxxx-----  ->  110   1  1xxxxxxx
  // 1 001xxxxxxxx----  ->  0 001xxxxxxxx----  ->  101   1  1xxxxxxx
  // 1 0001xxxxxxxx---  ->  0 0001xxxxxxxx---  ->  100   1  1xxxxxxx
  // 1 00001xxxxxxxx--  ->  0 00001xxxxxxxx--  ->  011   1  1xxxxxxx
  // 1 000001xxxxxxxx-  ->  0 000001xxxxxxxx-  ->  010   1  1xxxxxxx
  // 1 000000xxxxxxxxx  ->  0 000000xxxxxxxxx  ->  001   1  xxxxxxxx
  // 0 111111xxxxxxxxx  ->  1 111111xxxxxxxxx  ->  001   0  xxxxxxxx
  // 0 111110xxxxxxxx-  ->  1 111110xxxxxxxx-  ->  010   0  0xxxxxxx
  // 0 11110xxxxxxxx--  ->  1 11110xxxxxxxx--  ->  011   0  0xxxxxxx
  // 0 1110xxxxxxxx---  ->  1 1110xxxxxxxx---  ->  100   0  0xxxxxxx
  // 0 110xxxxxxxx----  ->  1 110xxxxxxxx----  ->  101   0  0xxxxxxx
  // 0 10xxxxxxxx-----  ->  1 10xxxxxxxx-----  ->  110   0  0xxxxxxx
  // 0 0xxxxxxxx------  ->  1 0xxxxxxxx------  ->  111   0  0xxxxxxx

  //-------------------------------------------------
  //  roundtrip_fp - compute the result of a round
  //  trip through the encode/decode process above
  //-------------------------------------------------
  public static /*sint16*/ int roundtrip_fp (/*sint32*/ int value) {
    // handle overflows first
    if (value < -32768) {
      return sint16 (-32768);
    }
    if (value > 32767) {
      return sint16 (32767);
    }

    // we need to count the number of leading sign bits after the sign
    // we can use count_leading_zeros if we invert negative values
    /*sint32*/ int scanvalue = value ^ (value >> 31);

    // exponent is related to the number of leading bits starting from bit 14
    int exponent = 7 - count_leading_zeros (scanvalue << 17);

    // smallest exponent value allowed is 1
    exponent = Math.max (exponent, 1);

    // apply the shift back and forth to zero out bits that are lost
    exponent -= 1;
    return sint16 ((value >> exponent) << exponent);
  }



  // variants
  // the YM2164 is almost 100% functionally identical to the YM2151, except
  // it apparently has some mystery registers in the 00-07 range, and timer
  // B's frequency is half that of the 2151

  // constants
  // the following constants need to be defined per family:
  //          uint OUTPUTS: The number of outputs exposed (1-4)
  //         uint CHANNELS: The number of channels on the chip
  //     uint ALL_CHANNELS: A bitmask of all channels
  //        uint OPERATORS: The number of operators on the chip
  //        uint WAVEFORMS: The number of waveforms offered
  //        uint REGISTERS: The number of 8-bit registers allocated
  // uint DEFAULT_PRESCALE: The starting clock prescale
  // uint EG_CLOCK_DIVIDER: The clock divider of the envelope generator
  // uint CSM_TRIGGER_MASK: Mask of channels to trigger in CSM mode
  //         uint REG_MODE: The address of the "mode" register controlling timers
  //     uint8_t STATUS_TIMERA: Status bit to set when timer A fires
  //     uint8_t STATUS_TIMERB: Status bit to set when tiemr B fires
  //       uint8_t STATUS_BUSY: Status bit to set when the chip is busy
  //        uint8_t STATUS_IRQ: Status bit to set when an IRQ is signalled
  public static final /*uint32*/ int OUTPUTS = 2;
  public static final /*uint32*/ int CHANNELS = 8;
  public static final /*uint32*/ int ALL_CHANNELS = (1 << CHANNELS) - 1;
  public static final /*uint32*/ int OPERATORS = CHANNELS * 4;
  public static final /*uint32*/ int WAVEFORMS = 1;
  public static final /*uint32*/ int REGISTERS = 0x100;
  public static final /*uint32*/ int DEFAULT_PRESCALE = 2;
  public static final /*uint32*/ int EG_CLOCK_DIVIDER = 3;
  public static final /*uint32*/ int CSM_TRIGGER_MASK = ALL_CHANNELS;
  public static final /*uint32*/ int REG_MODE = 0x14;
  public static final /*uint8*/ int STATUS_TIMERA = 0x01;
  public static final /*uint8*/ int STATUS_TIMERB = 0x02;
  public static final /*uint8*/ int STATUS_BUSY = 0x80;
  public static final /*uint8*/ int STATUS_IRQ = 0;

  // various envelope states
  //envelope_state
  public static final int EG_ATTACK = 1;
  public static final int EG_DECAY = 2;
  public static final int EG_SUSTAIN = 3;
  public static final int EG_RELEASE = 4;
  public static final int EG_STATES = 6;

  // external I/O access classes
  //access_class

  // this value is returned from the write() function for rhythm channels

  // this is the size of a full sin waveform
  public static final /*uint32*/ int WAVEFORM_LENGTH = 0x400;

  //struct opdata_cache
  // set phase_step to this value to recalculate it each sample; needed
  // in the case of PM LFO changes
  public static final /*uint32*/ int PHASE_STEP_DYNAMIC = 1;

  // "quiet" value, used to optimize when we can skip doing work
  public static final /*uint32*/ int EG_QUIET = 0x380;

  // the values here are stored as 4.8 logarithmic values for 1/4 phase
  // this matches the internal format of the OPN chip, extracted from the die
  public static final /*uint16*/ int[] s_sin_table = {
    0x859, 0x6c3, 0x607, 0x58b, 0x52e, 0x4e4, 0x4a6, 0x471, 0x443, 0x41a, 0x3f5, 0x3d3, 0x3b5, 0x398, 0x37e, 0x365,
    0x34e, 0x339, 0x324, 0x311, 0x2ff, 0x2ed, 0x2dc, 0x2cd, 0x2bd, 0x2af, 0x2a0, 0x293, 0x286, 0x279, 0x26d, 0x261,
    0x256, 0x24b, 0x240, 0x236, 0x22c, 0x222, 0x218, 0x20f, 0x206, 0x1fd, 0x1f5, 0x1ec, 0x1e4, 0x1dc, 0x1d4, 0x1cd,
    0x1c5, 0x1be, 0x1b7, 0x1b0, 0x1a9, 0x1a2, 0x19b, 0x195, 0x18f, 0x188, 0x182, 0x17c, 0x177, 0x171, 0x16b, 0x166,
    0x160, 0x15b, 0x155, 0x150, 0x14b, 0x146, 0x141, 0x13c, 0x137, 0x133, 0x12e, 0x129, 0x125, 0x121, 0x11c, 0x118,
    0x114, 0x10f, 0x10b, 0x107, 0x103, 0x0ff, 0x0fb, 0x0f8, 0x0f4, 0x0f0, 0x0ec, 0x0e9, 0x0e5, 0x0e2, 0x0de, 0x0db,
    0x0d7, 0x0d4, 0x0d1, 0x0cd, 0x0ca, 0x0c7, 0x0c4, 0x0c1, 0x0be, 0x0bb, 0x0b8, 0x0b5, 0x0b2, 0x0af, 0x0ac, 0x0a9,
    0x0a7, 0x0a4, 0x0a1, 0x09f, 0x09c, 0x099, 0x097, 0x094, 0x092, 0x08f, 0x08d, 0x08a, 0x088, 0x086, 0x083, 0x081,
    0x07f, 0x07d, 0x07a, 0x078, 0x076, 0x074, 0x072, 0x070, 0x06e, 0x06c, 0x06a, 0x068, 0x066, 0x064, 0x062, 0x060,
    0x05e, 0x05c, 0x05b, 0x059, 0x057, 0x055, 0x053, 0x052, 0x050, 0x04e, 0x04d, 0x04b, 0x04a, 0x048, 0x046, 0x045,
    0x043, 0x042, 0x040, 0x03f, 0x03e, 0x03c, 0x03b, 0x039, 0x038, 0x037, 0x035, 0x034, 0x033, 0x031, 0x030, 0x02f,
    0x02e, 0x02d, 0x02b, 0x02a, 0x029, 0x028, 0x027, 0x026, 0x025, 0x024, 0x023, 0x022, 0x021, 0x020, 0x01f, 0x01e,
    0x01d, 0x01c, 0x01b, 0x01a, 0x019, 0x018, 0x017, 0x017, 0x016, 0x015, 0x014, 0x014, 0x013, 0x012, 0x011, 0x011,
    0x010, 0x00f, 0x00f, 0x00e, 0x00d, 0x00d, 0x00c, 0x00c, 0x00b, 0x00a, 0x00a, 0x009, 0x009, 0x008, 0x008, 0x007,
    0x007, 0x007, 0x006, 0x006, 0x005, 0x005, 0x005, 0x004, 0x004, 0x004, 0x003, 0x003, 0x003, 0x002, 0x002, 0x002,
    0x002, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000,
  };

  private static int X (int a) {
    return (a | 0x400) << 2;
  }
  public static final /*uint16*/ int[] s_power_table = {
    X (0x3fa), X (0x3f5), X (0x3ef), X (0x3ea), X (0x3e4), X (0x3df), X (0x3da), X (0x3d4),
    X (0x3cf), X (0x3c9), X (0x3c4), X (0x3bf), X (0x3b9), X (0x3b4), X (0x3ae), X (0x3a9),
    X (0x3a4), X (0x39f), X (0x399), X (0x394), X (0x38f), X (0x38a), X (0x384), X (0x37f),
    X (0x37a), X (0x375), X (0x370), X (0x36a), X (0x365), X (0x360), X (0x35b), X (0x356),
    X (0x351), X (0x34c), X (0x347), X (0x342), X (0x33d), X (0x338), X (0x333), X (0x32e),
    X (0x329), X (0x324), X (0x31f), X (0x31a), X (0x315), X (0x310), X (0x30b), X (0x306),
    X (0x302), X (0x2fd), X (0x2f8), X (0x2f3), X (0x2ee), X (0x2e9), X (0x2e5), X (0x2e0),
    X (0x2db), X (0x2d6), X (0x2d2), X (0x2cd), X (0x2c8), X (0x2c4), X (0x2bf), X (0x2ba),
    X (0x2b5), X (0x2b1), X (0x2ac), X (0x2a8), X (0x2a3), X (0x29e), X (0x29a), X (0x295),
    X (0x291), X (0x28c), X (0x288), X (0x283), X (0x27f), X (0x27a), X (0x276), X (0x271),
    X (0x26d), X (0x268), X (0x264), X (0x25f), X (0x25b), X (0x257), X (0x252), X (0x24e),
    X (0x249), X (0x245), X (0x241), X (0x23c), X (0x238), X (0x234), X (0x230), X (0x22b),
    X (0x227), X (0x223), X (0x21e), X (0x21a), X (0x216), X (0x212), X (0x20e), X (0x209),
    X (0x205), X (0x201), X (0x1fd), X (0x1f9), X (0x1f5), X (0x1f0), X (0x1ec), X (0x1e8),
    X (0x1e4), X (0x1e0), X (0x1dc), X (0x1d8), X (0x1d4), X (0x1d0), X (0x1cc), X (0x1c8),
    X (0x1c4), X (0x1c0), X (0x1bc), X (0x1b8), X (0x1b4), X (0x1b0), X (0x1ac), X (0x1a8),
    X (0x1a4), X (0x1a0), X (0x19c), X (0x199), X (0x195), X (0x191), X (0x18d), X (0x189),
    X (0x185), X (0x181), X (0x17e), X (0x17a), X (0x176), X (0x172), X (0x16f), X (0x16b),
    X (0x167), X (0x163), X (0x160), X (0x15c), X (0x158), X (0x154), X (0x151), X (0x14d),
    X (0x149), X (0x146), X (0x142), X (0x13e), X (0x13b), X (0x137), X (0x134), X (0x130),
    X (0x12c), X (0x129), X (0x125), X (0x122), X (0x11e), X (0x11b), X (0x117), X (0x114),
    X (0x110), X (0x10c), X (0x109), X (0x106), X (0x102), X (0x0ff), X (0x0fb), X (0x0f8),
    X (0x0f4), X (0x0f1), X (0x0ed), X (0x0ea), X (0x0e7), X (0x0e3), X (0x0e0), X (0x0dc),
    X (0x0d9), X (0x0d6), X (0x0d2), X (0x0cf), X (0x0cc), X (0x0c8), X (0x0c5), X (0x0c2),
    X (0x0be), X (0x0bb), X (0x0b8), X (0x0b5), X (0x0b1), X (0x0ae), X (0x0ab), X (0x0a8),
    X (0x0a4), X (0x0a1), X (0x09e), X (0x09b), X (0x098), X (0x094), X (0x091), X (0x08e),
    X (0x08b), X (0x088), X (0x085), X (0x082), X (0x07e), X (0x07b), X (0x078), X (0x075),
    X (0x072), X (0x06f), X (0x06c), X (0x069), X (0x066), X (0x063), X (0x060), X (0x05d),
    X (0x05a), X (0x057), X (0x054), X (0x051), X (0x04e), X (0x04b), X (0x048), X (0x045),
    X (0x042), X (0x03f), X (0x03c), X (0x039), X (0x036), X (0x033), X (0x030), X (0x02d),
    X (0x02a), X (0x028), X (0x025), X (0x022), X (0x01f), X (0x01c), X (0x019), X (0x016),
    X (0x014), X (0x011), X (0x00e), X (0x00b), X (0x008), X (0x006), X (0x003), X (0x000),
  };

  public static final /*uint32*/ int[] s_increment_table = {
    0x00000000, 0x00000000, 0x10101010, 0x10101010,  // 0-3    (0x00-0x03)
    0x10101010, 0x10101010, 0x11101110, 0x11101110,  // 4-7    (0x04-0x07)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 8-11   (0x08-0x0B)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 12-15  (0x0C-0x0F)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 16-19  (0x10-0x13)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 20-23  (0x14-0x17)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 24-27  (0x18-0x1B)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 28-31  (0x1C-0x1F)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 32-35  (0x20-0x23)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 36-39  (0x24-0x27)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 40-43  (0x28-0x2B)
    0x10101010, 0x10111010, 0x11101110, 0x11111110,  // 44-47  (0x2C-0x2F)
    0x11111111, 0x21112111, 0x21212121, 0x22212221,  // 48-51  (0x30-0x33)
    0x22222222, 0x42224222, 0x42424242, 0x44424442,  // 52-55  (0x34-0x37)
    0x44444444, 0x84448444, 0x84848484, 0x88848884,  // 56-59  (0x38-0x3B)
    0x88888888, 0x88888888, 0x88888888, 0x88888888,  // 60-63  (0x3C-0x3F)
  };

  public static final /*uint8*/ int[] s_detune_adjustment = {
    0,  0,  1,  2,  0,  0,  1,  2,  0,  0,  1,  2,  0,  0,  1,  2,
    0,  1,  2,  2,  0,  1,  2,  3,  0,  1,  2,  3,  0,  1,  2,  3,
    0,  1,  2,  4,  0,  1,  3,  4,  0,  1,  3,  4,  0,  1,  3,  5,
    0,  2,  4,  5,  0,  2,  4,  6,  0,  2,  4,  6,  0,  2,  5,  7,
    0,  2,  5,  8,  0,  3,  6,  8,  0,  3,  6,  9,  0,  3,  7, 10,
    0,  4,  8, 11,  0,  4,  8, 12,  0,  4,  9, 13,  0,  5, 10, 14,
    0,  5, 11, 16,  0,  6, 12, 17,  0,  6, 13, 19,  0,  7, 14, 20,
    0,  8, 16, 22,  0,  8, 16, 22,  0,  8, 16, 22,  0,  8, 16, 22,
  };

  public static final /*uint32*/ int[] s_phase_step = {
    41568, 41600, 41632, 41664, 41696, 41728, 41760, 41792, 41856, 41888, 41920, 41952, 42016, 42048, 42080, 42112,
    42176, 42208, 42240, 42272, 42304, 42336, 42368, 42400, 42464, 42496, 42528, 42560, 42624, 42656, 42688, 42720,
    42784, 42816, 42848, 42880, 42912, 42944, 42976, 43008, 43072, 43104, 43136, 43168, 43232, 43264, 43296, 43328,
    43392, 43424, 43456, 43488, 43552, 43584, 43616, 43648, 43712, 43744, 43776, 43808, 43872, 43904, 43936, 43968,
    44032, 44064, 44096, 44128, 44192, 44224, 44256, 44288, 44352, 44384, 44416, 44448, 44512, 44544, 44576, 44608,
    44672, 44704, 44736, 44768, 44832, 44864, 44896, 44928, 44992, 45024, 45056, 45088, 45152, 45184, 45216, 45248,
    45312, 45344, 45376, 45408, 45472, 45504, 45536, 45568, 45632, 45664, 45728, 45760, 45792, 45824, 45888, 45920,
    45984, 46016, 46048, 46080, 46144, 46176, 46208, 46240, 46304, 46336, 46368, 46400, 46464, 46496, 46528, 46560,
    46656, 46688, 46720, 46752, 46816, 46848, 46880, 46912, 46976, 47008, 47072, 47104, 47136, 47168, 47232, 47264,
    47328, 47360, 47392, 47424, 47488, 47520, 47552, 47584, 47648, 47680, 47744, 47776, 47808, 47840, 47904, 47936,
    48032, 48064, 48096, 48128, 48192, 48224, 48288, 48320, 48384, 48416, 48448, 48480, 48544, 48576, 48640, 48672,
    48736, 48768, 48800, 48832, 48896, 48928, 48992, 49024, 49088, 49120, 49152, 49184, 49248, 49280, 49344, 49376,
    49440, 49472, 49504, 49536, 49600, 49632, 49696, 49728, 49792, 49824, 49856, 49888, 49952, 49984, 50048, 50080,
    50144, 50176, 50208, 50240, 50304, 50336, 50400, 50432, 50496, 50528, 50560, 50592, 50656, 50688, 50752, 50784,
    50880, 50912, 50944, 50976, 51040, 51072, 51136, 51168, 51232, 51264, 51328, 51360, 51424, 51456, 51488, 51520,
    51616, 51648, 51680, 51712, 51776, 51808, 51872, 51904, 51968, 52000, 52064, 52096, 52160, 52192, 52224, 52256,
    52384, 52416, 52448, 52480, 52544, 52576, 52640, 52672, 52736, 52768, 52832, 52864, 52928, 52960, 52992, 53024,
    53120, 53152, 53216, 53248, 53312, 53344, 53408, 53440, 53504, 53536, 53600, 53632, 53696, 53728, 53792, 53824,
    53920, 53952, 54016, 54048, 54112, 54144, 54208, 54240, 54304, 54336, 54400, 54432, 54496, 54528, 54592, 54624,
    54688, 54720, 54784, 54816, 54880, 54912, 54976, 55008, 55072, 55104, 55168, 55200, 55264, 55296, 55360, 55392,
    55488, 55520, 55584, 55616, 55680, 55712, 55776, 55808, 55872, 55936, 55968, 56032, 56064, 56128, 56160, 56224,
    56288, 56320, 56384, 56416, 56480, 56512, 56576, 56608, 56672, 56736, 56768, 56832, 56864, 56928, 56960, 57024,
    57120, 57152, 57216, 57248, 57312, 57376, 57408, 57472, 57536, 57568, 57632, 57664, 57728, 57792, 57824, 57888,
    57952, 57984, 58048, 58080, 58144, 58208, 58240, 58304, 58368, 58400, 58464, 58496, 58560, 58624, 58656, 58720,
    58784, 58816, 58880, 58912, 58976, 59040, 59072, 59136, 59200, 59232, 59296, 59328, 59392, 59456, 59488, 59552,
    59648, 59680, 59744, 59776, 59840, 59904, 59936, 60000, 60064, 60128, 60160, 60224, 60288, 60320, 60384, 60416,
    60512, 60544, 60608, 60640, 60704, 60768, 60800, 60864, 60928, 60992, 61024, 61088, 61152, 61184, 61248, 61280,
    61376, 61408, 61472, 61536, 61600, 61632, 61696, 61760, 61824, 61856, 61920, 61984, 62048, 62080, 62144, 62208,
    62272, 62304, 62368, 62432, 62496, 62528, 62592, 62656, 62720, 62752, 62816, 62880, 62944, 62976, 63040, 63104,
    63200, 63232, 63296, 63360, 63424, 63456, 63520, 63584, 63648, 63680, 63744, 63808, 63872, 63904, 63968, 64032,
    64096, 64128, 64192, 64256, 64320, 64352, 64416, 64480, 64544, 64608, 64672, 64704, 64768, 64832, 64896, 64928,
    65024, 65056, 65120, 65184, 65248, 65312, 65376, 65408, 65504, 65536, 65600, 65664, 65728, 65792, 65856, 65888,
    65984, 66016, 66080, 66144, 66208, 66272, 66336, 66368, 66464, 66496, 66560, 66624, 66688, 66752, 66816, 66848,
    66944, 66976, 67040, 67104, 67168, 67232, 67296, 67328, 67424, 67456, 67520, 67584, 67648, 67712, 67776, 67808,
    67904, 67936, 68000, 68064, 68128, 68192, 68256, 68288, 68384, 68448, 68512, 68544, 68640, 68672, 68736, 68800,
    68896, 68928, 68992, 69056, 69120, 69184, 69248, 69280, 69376, 69440, 69504, 69536, 69632, 69664, 69728, 69792,
    69920, 69952, 70016, 70080, 70144, 70208, 70272, 70304, 70400, 70464, 70528, 70560, 70656, 70688, 70752, 70816,
    70912, 70976, 71040, 71104, 71136, 71232, 71264, 71360, 71424, 71488, 71552, 71616, 71648, 71744, 71776, 71872,
    71968, 72032, 72096, 72160, 72192, 72288, 72320, 72416, 72480, 72544, 72608, 72672, 72704, 72800, 72832, 72928,
    72992, 73056, 73120, 73184, 73216, 73312, 73344, 73440, 73504, 73568, 73632, 73696, 73728, 73824, 73856, 73952,
    74080, 74144, 74208, 74272, 74304, 74400, 74432, 74528, 74592, 74656, 74720, 74784, 74816, 74912, 74944, 75040,
    75136, 75200, 75264, 75328, 75360, 75456, 75488, 75584, 75648, 75712, 75776, 75840, 75872, 75968, 76000, 76096,
    76224, 76288, 76352, 76416, 76448, 76544, 76576, 76672, 76736, 76800, 76864, 76928, 77024, 77120, 77152, 77248,
    77344, 77408, 77472, 77536, 77568, 77664, 77696, 77792, 77856, 77920, 77984, 78048, 78144, 78240, 78272, 78368,
    78464, 78528, 78592, 78656, 78688, 78784, 78816, 78912, 78976, 79040, 79104, 79168, 79264, 79360, 79392, 79488,
    79616, 79680, 79744, 79808, 79840, 79936, 79968, 80064, 80128, 80192, 80256, 80320, 80416, 80512, 80544, 80640,
    80768, 80832, 80896, 80960, 80992, 81088, 81120, 81216, 81280, 81344, 81408, 81472, 81568, 81664, 81696, 81792,
    81952, 82016, 82080, 82144, 82176, 82272, 82304, 82400, 82464, 82528, 82592, 82656, 82752, 82848, 82880, 82976,
  };

  // this table encodes 2 shift values to apply to the top 7 bits
  // of fnum; it is effectively a cheap multiply by a constant
  // value containing 0-2 bits
  public static final /*uint8*/ int[] s_lfo_pm_shifts = {
    0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
    0x77, 0x77, 0x77, 0x77, 0x72, 0x72, 0x72, 0x72,
    0x77, 0x77, 0x77, 0x72, 0x72, 0x72, 0x17, 0x17,
    0x77, 0x77, 0x72, 0x72, 0x17, 0x17, 0x12, 0x12,
    0x77, 0x77, 0x72, 0x17, 0x17, 0x17, 0x12, 0x07,
    0x77, 0x77, 0x17, 0x12, 0x07, 0x07, 0x02, 0x01,
    0x77, 0x77, 0x17, 0x12, 0x07, 0x07, 0x02, 0x01,
    0x77, 0x77, 0x17, 0x12, 0x07, 0x07, 0x02, 0x01,
  };

  // LFO waveforms are 256 entries long
  public static final /*uint32*/ int LFO_WAVEFORM_LENGTH = 256;

  // helper to encode four operator numbers into a 32-bit value in the
  // operator maps for each register class
  //fm_registers_base::operator_list
  private static /*uint32*/ int operator_list (/*uint8*/ int o1, /*uint8*/ int o2, /*uint8*/ int o3, /*uint8*/ int o4) {
    return o1 | (o2 << 8) | (o3 << 16) | (o4 << 24);
  }

  // Note that the channel index order is 0,2,1,3, so we bitswap the index.
  //
  // This is because the order in the map is:
  //    carrier 1, carrier 2, modulator 1, modulator 2
  //
  // But when wiring up the connections, the more natural order is:
  //    carrier 1, modulator 1, carrier 2, modulator 2
  //opm_registers::operator_map
  public static final /*operator_mapping*/ int[] s_fixed_map = {
    operator_list (  0, 16,  8, 24 ),  // Channel 0 operators
    operator_list (  1, 17,  9, 25 ),  // Channel 1 operators
    operator_list (  2, 18, 10, 26 ),  // Channel 2 operators
    operator_list (  3, 19, 11, 27 ),  // Channel 3 operators
    operator_list (  4, 20, 12, 28 ),  // Channel 4 operators
    operator_list (  5, 21, 13, 29 ),  // Channel 5 operators
    operator_list (  6, 22, 14, 30 ),  // Channel 6 operators
    operator_list (  7, 23, 15, 31 ),  // Channel 7 operators
  };

  //fm_channel::output_4op
  private static int ALGORITHM (int op2in, int op3in, int op4in, int op1out, int op2out, int op3out) {
    return op2in | (op3in << 1) | (op4in << 4) | (op1out << 7) | (op2out << 8) | (op3out << 9);
  }
  public static final /*uint16*/ int[] s_algorithm_ops = {
    ALGORITHM (1,2,3, 0,0,0),  //  0: O1 -> O2 -> O3 -> O4 -> out (O4)
    ALGORITHM (0,5,3, 0,0,0),  //  1: (O1 + O2) -> O3 -> O4 -> out (O4)
    ALGORITHM (0,2,6, 0,0,0),  //  2: (O1 + (O2 -> O3)) -> O4 -> out (O4)
    ALGORITHM (1,0,7, 0,0,0),  //  3: ((O1 -> O2) + O3) -> O4 -> out (O4)
    ALGORITHM (1,0,3, 0,1,0),  //  4: ((O1 -> O2) + (O3 -> O4)) -> out (O2+O4)
    ALGORITHM (1,1,1, 0,1,1),  //  5: ((O1 -> O2) + (O1 -> O3) + (O1 -> O4)) -> out (O2+O3+O4)
    ALGORITHM (1,0,0, 0,1,1),  //  6: ((O1 -> O2) + O3 + O4) -> out (O2+O3+O4)
    ALGORITHM (0,0,0, 1,1,1),  //  7: (O1 + O2 + O3 + O4) -> out (O1+O2+O3+O4)
    ALGORITHM (1,2,3, 0,0,0),  //  8: O1 -> O2 -> O3 -> O4 -> out (O4)         [same as 0]
    ALGORITHM (0,2,3, 1,0,0),  //  9: (O1 + (O2 -> O3 -> O4)) -> out (O1+O4)   [unique]
    ALGORITHM (1,0,3, 0,1,0),  // 10: ((O1 -> O2) + (O3 -> O4)) -> out (O2+O4) [same as 4]
    ALGORITHM (0,2,0, 1,0,1),  // 11: (O1 + (O2 -> O3) + O4) -> out (O1+O3+O4) [unique]
  };

  public static final /*sint16*/ int[] s_detune2_delta = { 0, (600*64+50)/100, (781*64+50)/100, (950*64+50)/100 };

  // three different keyon sources; actual keyon is an OR over all of these
  //keyon_type
  public static final int KEYON_NORMAL = 0;
  public static final int KEYON_RHYTHM = 1;
  public static final int KEYON_CSM = 2;



  //-------------------------------------------------
  //  abs_sin_attenuation - given a sin (phase) input
  //  where the range 0-2*PI is mapped onto 10 bits,
  //  return the absolute value of sin(input),
  //  logarithmically-adjusted and treated as an
  //  attenuation value, in 4.8 fixed point format
  //-------------------------------------------------
  public static /*uint32*/ int abs_sin_attenuation (/*uint32*/ int input) {
    // if the top bit is set, we're in the second half of the curve
    // which is a mirror image, so invert the index
    if (bitfield (input, 8) != 0) {
      input = ~input;
    }
    // return the value from the table
    return s_sin_table[input & 0xff];
  }

  //-------------------------------------------------
  //  attenuation_to_volume - given a 5.8 fixed point
  //  logarithmic attenuation value, return a 13-bit
  //  linear volume
  //-------------------------------------------------
  public static /*uint32*/ int attenuation_to_volume (/*uint32*/ int input) {
    // the values here are 10-bit mantissas with an implied leading bit
    // this matches the internal format of the OPN chip, extracted from the die

    // as a nod to performance, the implicit 0x400 bit is pre-incorporated, and
    // the values are left-shifted by 2 so that a simple right shift is all that
    // is needed; also the order is reversed to save a NOT on the input

    // look up the fractional part, then shift by the whole
    return s_power_table[input & 0xff] >>> (input >>> 8);
  }

  //-------------------------------------------------
  //  attenuation_increment - given a 6-bit ADSR
  //  rate value and a 3-bit stepping index,
  //  return a 4-bit increment to the attenutaion
  //  for this step (or for the attack case, the
  //  fractional scale factor to decrease by)
  //-------------------------------------------------
  public static /*uint32*/ int attenuation_increment (/*uint32*/ int rate, /*uint32*/ int index) {
    return bitfield (s_increment_table[rate], 4 * index, 4);
  }

  //-------------------------------------------------
  //  detune_adjustment - given a 5-bit key code
  //  value and a 3-bit detune parameter, return a
  //  6-bit signed phase displacement; this table
  //  has been verified against Nuked's equations,
  //  but the equations are rather complicated, so
  //  we'll keep the simplicity of the table
  //-------------------------------------------------
  public static /*sint32*/ int detune_adjustment (/*uint32*/ int detune, /*uint32*/ int keycode) {
    /*sint32*/ int result = s_detune_adjustment[4 * keycode + (detune & 3)];
    return bitfield (detune, 2) != 0 ? -result : result;
  }

  //-------------------------------------------------
  //  opm_key_code_to_phase_step - converts an
  //  OPM concatenated block (3 bits), keycode
  //  (4 bits) and key fraction (6 bits) to a 0.10
  //  phase step, after applying the given delta;
  //  this applies to OPM and OPZ, so it lives here
  //  in a central location
  //-------------------------------------------------
  public static /*uint32*/ int opm_key_code_to_phase_step (/*uint32*/ int block_freq, /*sint32*/ int delta) {
    // The phase step is essentially the fnum in OPN-speak. To compute this table,
    // we used the standard formula for computing the frequency of a note, and
    // then converted that frequency to fnum using the formula documented in the
    // YM2608 manual.
    //
    // However, the YM2608 manual describes everything in terms of a nominal 8MHz
    // clock, which produces an FM clock of:
    //
    //    8000000 / 24(operators) / 6(prescale) = 55555Hz FM clock
    //
    // Whereas the descriptions for the YM2151 use a nominal 3.579545MHz clock:
    //
    //    3579545 / 32(operators) / 2(prescale) = 55930Hz FM clock
    //
    // To correct for this, the YM2608 formula was adjusted to use a clock of
    // 8053920Hz, giving this equation for the fnum:
    //
    //    fnum = (double(144) * freq * (1 << 20)) / double(8053920) / 4;
    //
    // Unfortunately, the computed table differs in a few spots from the data
    // verified from an actual chip. The table below comes from David Viens'
    // analysis, used with his permission.

    // extract the block (octave) first
    /*uint32*/ int block = bitfield (block_freq, 10, 3);

    // the keycode (bits 6-9) is "gappy", mapping 12 values over 16 in each
    // octave; to correct for this, we multiply the 4-bit value by 3/4 (or
    // rather subtract 1/4); note that a (invalid) value of 15 will bleed into
    // the next octave -- this is confirmed
    /*uint32*/ int adjusted_code = bitfield (block_freq, 6, 4) - bitfield (block_freq, 8, 2);

    // now re-insert the 6-bit fraction
    /*sint32*/ int eff_freq = (adjusted_code << 6) | bitfield (block_freq, 0, 6);

    // now that the gaps are removed, add the delta
    eff_freq += delta;

    // handle over/underflow by adjusting the block:
    if (Integer.compareUnsigned (eff_freq, 768) >= 0) {
      // minimum delta is -512 (PM), so we can only underflow by 1 octave
      if (eff_freq < 0) {
        eff_freq += 768;
        if (block-- == 0) {
          return s_phase_step[0] >>> 7;
        }
      }

      // maximum delta is +512+608 (PM+detune), so we can overflow by up to 2 octaves
      else {
        eff_freq -= 768;
        if (eff_freq >= 768) {
          block++;
          eff_freq -= 768;
        }
        if (block++ >= 7) {
          return s_phase_step[767];
        }
      }
    }

    // look up the phase shift for the key code, then shift by octave
    return s_phase_step[eff_freq] >>> (block ^ 7);
  }

  //-------------------------------------------------
  //  opn_lfo_pm_phase_adjustment - given the 7 most
  //  significant frequency number bits, plus a 3-bit
  //  PM depth value and a signed 5-bit raw PM value,
  //  return a signed PM adjustment to the frequency;
  //  algorithm written to match Nuked behavior
  //-------------------------------------------------
  public static /*sint32*/ int opn_lfo_pm_phase_adjustment (/*uint32*/ int fnum_bits, /*uint32*/ int pm_sensitivity, /*sint32*/ int lfo_raw_pm) {
    // look up the relevant shifts
    /*sint32*/ int abs_pm = (lfo_raw_pm < 0) ? -lfo_raw_pm : lfo_raw_pm;
    /*uint32*/ int shifts = s_lfo_pm_shifts[8 * pm_sensitivity + bitfield (abs_pm, 0, 3)];

    // compute the adjustment
    /*sint32*/ int adjust = (fnum_bits >>> bitfield (shifts, 0, 4)) + (fnum_bits >>> bitfield (shifts, 4, 4));
    if (pm_sensitivity > 5) {
      adjust <<= pm_sensitivity - 5;
    }
    adjust >>= 2;

    // every 16 cycles it inverts sign
    return (lfo_raw_pm < 0) ? -adjust : adjust;
  }



  // this class holds data that is computed once at the start of clocking
  // and remains static during subsequent sound generation
  //struct opdata_cache
  class opdata_cache {
    /*uint16*/ int[] waveform;  // base of sine table
    /*uint32*/ int phase_step;  // phase step, or PHASE_STEP_DYNAMIC if PM is active
    /*uint32*/ int total_level;  // total level * 8 + KSL
    /*uint32*/ int block_freq;  // raw block frequency value (used to compute phase_step)
    /*sint32*/ int detune;  // detuning value (used to compute phase_step)
    /*uint32*/ int multiple;  // multiple value (x.1, used to compute phase_step)
    /*uint32*/ int eg_sustain;  // sustain level, shifted up to envelope values
    /*uint8*/ int[] eg_rate;  // envelope rate, including KSR
    /*uint8*/ int eg_shift;  // envelope shift amount
    public opdata_cache () {
      eg_rate = new /*uint8*/ int[EG_STATES];
      eg_shift = 0;
    }
  }



  //class fm_registers_base

  // helper to apply KSR to the raw ADSR rate, ignoring ksr if the
  // raw value is 0, and clamping to 63
  protected static /*uint32*/ int effective_rate (/*uint32*/ int rawrate, /*uint32*/ int ksr) {
    return (rawrate == 0) ? 0 : umin32 (rawrate + ksr, 63);
  }



  // fm_operator represents an FM operator (or "slot" in FM parlance), which
  // produces an output sine wave modulated by an envelope
  class fm_operator {

    // internal state
    private /*uint32*/ int m_choffs;  // channel offset in registers
    private /*uint32*/ int m_opoffs;  // operator offset in registers
    private /*uint32*/ int m_phase;  // current phase value (10.10 format)
    public /*uint16*/ int m_env_attenuation;  // computed envelope attenuation (4.6 format)
    public /*envelope_state*/ int m_env_state;  // current envelope state
    private /*uint8*/ int m_key_state;  // current key state: on or off (bit 0)
    private /*uint8*/ int m_keyon_live;  // live key on state (bit 0 = direct, bit 1 = rhythm, bit 2 = CSM)
    private opdata_cache m_cache;  // cached values for performance

    // constructor
    //-------------------------------------------------
    //  fm_operator - constructor
    //-------------------------------------------------
    public fm_operator (/*uint32*/ int opoffs) {
      m_choffs = 0;
      m_opoffs = opoffs;
      m_phase = 0;
      m_env_attenuation = uint16 (0x3ff);
      m_env_state = EG_RELEASE;
      m_key_state = uint8 (0);
      m_keyon_live = uint8 (0);
      m_cache = new opdata_cache ();
    }

    // reset the operator state
    //-------------------------------------------------
    //  reset - reset the channel state
    //-------------------------------------------------
    public void reset () {
      // reset our data
      m_phase = 0;
      m_env_attenuation = uint16 (0x3ff);
      m_env_state = EG_RELEASE;
      m_key_state = uint8 (0);
      m_keyon_live = uint8 (0);
    }

    // set the current channel
    public void set_choffs (/*uint32*/ int choffs) {
      m_choffs = choffs;
    }

    // prepare prior to clocking
    //-------------------------------------------------
    //  prepare - prepare for clocking
    //-------------------------------------------------
    public boolean prepare () {
      // cache the data
      cache_operator_data (m_choffs, m_opoffs, m_cache);

      // clock the key state
      clock_keystate (m_keyon_live != 0 ? 1 : 0);
      //m_keyon_live &= ~(1 << KEYON_CSM);
      m_keyon_live = uint8 (m_keyon_live & (~(1 << KEYON_CSM)));

      // we're active until we're quiet after the release
      return (m_env_state != EG_RELEASE || Integer.compareUnsigned (m_env_attenuation, EG_QUIET) < 0);
    }

    // master clocking function
    //-------------------------------------------------
    //  clock - master clocking function
    //-------------------------------------------------
    public void clock (/*uint32*/ int env_counter, /*sint32*/ int lfo_raw_pm) {
      // clock the envelope if on an envelope cycle; env_counter is a x.2 value
      if (bitfield (env_counter, 0, 2) == 0) {
        clock_envelope (env_counter >>> 2);
      }

      // clock the phase
      //-------------------------------------------------
      //  clock_phase - clock the 10.10 phase value; the
      //  OPN version of the logic has been verified
      //  against the Nuked phase generator
      //-------------------------------------------------
      //fm_operator::clock_phase
      // read from the cache, or recalculate if PM active
      /*uint32*/ int phase_step = m_cache.phase_step;
      if (phase_step == PHASE_STEP_DYNAMIC) {
        phase_step = compute_phase_step (m_choffs, m_opoffs, m_cache, lfo_raw_pm);
      }

      // finally apply the step to the current phase value
      m_phase += phase_step;
    }

    //fm_operator::phase
    // return the current phase value
    public /*uint32*/ int phase () {
      return m_phase >>> 10;
    }

    // compute operator volume
    //-------------------------------------------------
    //  compute_volume - compute the 14-bit signed
    //  volume of this operator, given a phase
    //  modulation and an AM LFO offset
    //-------------------------------------------------
    //fm_operator::compute_volume
    public /*sint32*/ int compute_volume (/*uint32*/ int phase, /*uint32*/ int am_offset) {
      // the low 10 bits of phase represents a full 2*PI period over
      // the full sin wave

      // early out if the envelope is effectively off
      if (Integer.compareUnsigned (m_env_attenuation, EG_QUIET) > 0) {
        return 0;
      }

      // get the absolute value of the sin, as attenuation, as a 4.8 fixed point value
      /*uint32*/ int sin_attenuation = m_cache.waveform[phase & (WAVEFORM_LENGTH - 1)];

      // get the attenuation from the evelope generator as a 4.6 value, shifted up to 4.8
      /*uint32*/ int env_attenuation = envelope_attenuation (am_offset) << 2;

      // combine into a 5.8 value, then convert from attenuation to 13-bit linear volume
      /*sint32*/ int result = attenuation_to_volume ((sin_attenuation & 0x7fff) + env_attenuation);

      // negate if in the negative part of the sin wave (sign bit gives 14 bits)
      return bitfield (sin_attenuation, 15) != 0 ? -result : result;
    }

    // compute volume for the OPM noise channel
    //-------------------------------------------------
    //  compute_noise_volume - compute the 14-bit
    //  signed noise volume of this operator, given a
    //  noise input value and an AM offset
    //-------------------------------------------------
    //fm_operator::compute_noise_volume
    public int compute_noise_volume (/*uint32*/ int am_offset) {
      // application manual says the logarithmic transform is not applied here, so we
      // just use the raw envelope attenuation, inverted (since 0 attenuation should be
      // maximum), and shift it up from a 10-bit value to an 11-bit value
      /*sint32*/ int result = (envelope_attenuation (am_offset) ^ 0x3ff) << 1;

      // QUESTION: is AM applied still?

      // negate based on the noise state
      return bitfield (noise_state (), 0) != 0 ? -result : result;
    }

    // key state control
    //-------------------------------------------------
    //  keyonoff - signal a key on/off event
    //-------------------------------------------------
    //fm_operator::keyonoff
    public void keyonoff (/*uint32*/ int on, /*keyon_type*/ int type) {
      m_keyon_live = uint8 ((m_keyon_live & ~(1 << type)) | (bitfield (on, 0) << type));
    }

    // start the attack phase
    //-------------------------------------------------
    //  start_attack - start the attack phase; called
    //  when a keyon happens or when an SSG-EG cycle
    //  is complete and restarts
    //-------------------------------------------------
    //fm_operator::start_attack
    private void start_attack () {
      start_attack (false);
    }
    private void start_attack (boolean is_restart) {
      // don't change anything if already in attack state
      if (m_env_state == EG_ATTACK) {
        return;
      }
      m_env_state = EG_ATTACK;

      // generally not inverted at start, except if SSG-EG is enabled and
      // one of the inverted modes is specified; leave this alone on a
      // restart, as it is managed by the clock_ssg_eg_state() code

      // reset the phase when we start an attack due to a key on
      // (but not when due to an SSG-EG restart except in certain cases
      // managed directly by the SSG-EG code)
      if (!is_restart) {
        m_phase = 0;
      }

      // if the attack rate >= 62 then immediately go to max attenuation
      if (Integer.compareUnsigned (m_cache.eg_rate[EG_ATTACK], 62) >= 0) {
        m_env_attenuation = uint16 (0);
      }
    }

    // start the release phase
    //-------------------------------------------------
    //  start_release - start the release phase;
    //  called when a keyoff happens
    //-------------------------------------------------
    //fm_operator::start_release
    private void start_release () {
      // don't change anything if already in release state
      if (m_env_state >= EG_RELEASE) {
        return;
      }
      m_env_state = EG_RELEASE;

      // if attenuation if inverted due to SSG-EG, snap the inverted attenuation
      // as the starting point
    }

    // clock phases
    //-------------------------------------------------
    //  clock_keystate - clock the keystate to match
    //  the incoming keystate
    //-------------------------------------------------
    //fm_operator::clock_keystate
    private void clock_keystate (/*uint32*/ int keystate) {

      // has the key changed?
      if ((keystate ^ m_key_state) != 0) {
        m_key_state = uint8 (keystate);

        // if the key has turned on, start the attack
        if (keystate != 0) {
          // OPLL has a DP ("depress"?) state to bring the volume
          // down before starting the attack
          start_attack ();
        }

        // otherwise, start the release
        else {
          start_release ();
        }
      }
    }

    //-------------------------------------------------
    //  clock_ssg_eg_state - clock the SSG-EG state;
    //  should only be called if SSG-EG is enabled
    //-------------------------------------------------
    //fm_operator::clock_ssg_eg_state

    //-------------------------------------------------
    //  clock_envelope - clock the envelope state
    //  according to the given count
    //-------------------------------------------------
    //fm_operator::clock_envelope
    private void clock_envelope (/*uint32*/ int env_counter) {
      // handle attack->decay transitions
      if (m_env_state == EG_ATTACK && m_env_attenuation == 0) {
        m_env_state = EG_DECAY;
      }

      // handle decay->sustain transitions; it is important to do this immediately
      // after the attack->decay transition above in the event that the sustain level
      // is set to 0 (in which case we will skip right to sustain without doing any
      // decay); as an example where this can be heard, check the cymbals sound
      // in channel 0 of shinobi's test mode sound #5
      if (m_env_state == EG_DECAY && Integer.compareUnsigned (m_env_attenuation, m_cache.eg_sustain) >= 0) {
        m_env_state = EG_SUSTAIN;
      }

      // fetch the appropriate 6-bit rate value from the cache
      /*uint32*/ int rate = m_cache.eg_rate[m_env_state];

      // compute the rate shift value; this is the shift needed to
      // apply to the env_counter such that it becomes a 5.11 fixed
      // point number
      /*uint32*/ int rate_shift = rate >>> 2;
      env_counter <<= rate_shift;

      // see if the fractional part is 0; if not, it's not time to clock
      if (bitfield (env_counter, 0, 11) != 0) {
        return;
      }

      // determine the increment based on the non-fractional part of env_counter
      /*uint32*/ int relevant_bits = bitfield (env_counter, (rate_shift <= 11) ? 11 : rate_shift, 3);
      /*uint32*/ int increment = attenuation_increment (rate, relevant_bits);

      // attack is the only one that increases
      if (m_env_state == EG_ATTACK) {
        // glitch means that attack rates of 62/63 don't increment if
        // changed after the initial key on (where they are handled
        // specially); nukeykt confirms this happens on OPM, OPN, OPL/OPLL
        // at least so assuming it is true for everyone
        if (rate < 62) {
          //m_env_attenuation += (~m_env_attenuation * increment) >>> 4;
          m_env_attenuation = uint16 (m_env_attenuation + ((~m_env_attenuation * increment) >>> 4));
        }
      }

      // all other cases are similar
      else {
        // non-SSG-EG cases just apply the increment
        //m_env_attenuation += increment;
        m_env_attenuation = uint16 (m_env_attenuation + increment);

        // SSG-EG only applies if less than mid-point, and then at 4x

        // clamp the final attenuation
        if (Integer.compareUnsigned (m_env_attenuation, 0x400) >= 0) {
          m_env_attenuation = uint16 (0x3ff);
        }

        // transition from depress to attack

        // transition from release to reverb, should switch at -18dB

      }
    }

    //fm_operator::clock_phase

    // return effective attenuation of the envelope
    //-------------------------------------------------
    //  envelope_attenuation - return the effective
    //  attenuation of the envelope
    //-------------------------------------------------
    //fm_operator::envelope_attenuation
    private /*uint32*/ int envelope_attenuation (/*uint32*/ int am_offset) {
      /*uint32*/ int result = m_env_attenuation >>> m_cache.eg_shift;

      // invert if necessary due to SSG-EG

      // add in LFO AM modulation
      if (op_lfo_am_enable (m_opoffs) != 0) {
        result += am_offset;
      }

      // add in total level and KSL from the cache
      result += m_cache.total_level;

      // clamp to max, apply shift, and return
      return umin32 (result, 0x3ff);
    }

  }  //class fm_operator



  // fm_channel represents an FM channel which combines the output of 2 or 4
  // operators into a final result
  class fm_channel {

    // internal state
    private /*uint32*/ int m_choffs;  // channel offset in registers
    private /*sint16*/ int m_feedback_0, m_feedback_1;  // feedback memory for operator 1
    private /*sint16*/ int m_feedback_in;  // next input value for op 1 feedback (set in output)
    public fm_operator[] m_op;  // up to 4 operators

    // constructor
    //-------------------------------------------------
    //  fm_channel - constructor
    //-------------------------------------------------
    //fm_channel::fm_channel
    public fm_channel (/*uint32*/ int choffs) {
      m_choffs = choffs;
      m_feedback_0 = m_feedback_1 = sint16 (0);
      m_feedback_in = sint16 (0);
      m_op = new fm_operator[4];
    }

    // reset the channel state
    //-------------------------------------------------
    //  reset - reset the channel state
    //-------------------------------------------------
    //fm_channel::reset
    public void reset () {
      // reset our data
      m_feedback_0 = m_feedback_1 = sint16 (0);
      m_feedback_in = sint16 (0);
    }

    //fm_channel::save_restore

    // assign operators
    public void assign (/*uint32*/ int index, fm_operator op) {
      m_op[index] = op;
      op.set_choffs (m_choffs);
    }

    // signal key on/off to our operators
    //-------------------------------------------------
    //  keyonoff - signal key on/off to our operators
    //-------------------------------------------------
    //fm_channel::keyonoff
    public void keyonoff (/*uint32*/ int states, /*keyon_type*/ int type, /*uint32*/ int chnum) {
      for (/*uint32*/ int opnum = 0; opnum < 4; opnum++) {
        m_op[opnum].keyonoff (bitfield (states, opnum), type);
      }
    }

    // prepare prior to clocking
    //-------------------------------------------------
    //  prepare - prepare for clocking
    //-------------------------------------------------
    //fm_channel::prepare
    public boolean prepare () {
      /*uint32*/ int active_mask = 0;

      // prepare all operators and determine if they are active
      for (/*uint32*/ int opnum = 0; opnum < 4; opnum++) {
        if (m_op[opnum].prepare ())  {
          active_mask |= 1 << opnum;
        }
      }

      return (active_mask != 0);
    }

    // master clocking function
    //-------------------------------------------------
    //  clock - master clock of all operators
    //-------------------------------------------------
    //fm_channel::clock
    public void clock (/*uint32*/ int env_counter, /*sint32*/ int lfo_raw_pm) {
      // clock the feedback through
      m_feedback_0 = sint16 (m_feedback_1);
      m_feedback_1 = sint16 (m_feedback_in);

      for (/*uint32*/ int opnum = 0; opnum < 4; opnum++) {
        m_op[opnum].clock (env_counter, lfo_raw_pm);
      }
    }

    //-------------------------------------------------
    //  output_2op - combine 4 operators according to
    //  the specified algorithm, returning a sum
    //  according to the rshift and clipmax parameters,
    //  which vary between different implementations
    //-------------------------------------------------
    //fm_channel::output_2op

    //-------------------------------------------------
    //  output_4op - combine 4 operators according to
    //  the specified algorithm, returning a sum
    //  according to the rshift and clipmax parameters,
    //  which vary between different implementations
    //-------------------------------------------------
    //fm_channel::output_4op
    public void output_4op (/*uint32*/ int rshift, /*sint32*/ int clipmax) {
      // all 4 operators should be populated

      // AM amount is the same across all operators; compute it once
      /*uint32*/ int am_offset = lfo_am_offset (m_choffs);

      // operator 1 has optional self-feedback
      /*sint32*/ int opmod = 0;
      /*uint32*/ int feedback = ch_feedback (m_choffs);
      if (feedback != 0) {
        opmod = (m_feedback_0 + m_feedback_1) >> (10 - feedback);
      }

      // compute the 14-bit volume/value of operator 1 and update the feedback
      /*sint32*/ int op1value = m_feedback_in = sint16 (m_op[0].compute_volume (m_op[0].phase () + opmod, am_offset));

      // now that the feedback has been computed, skip the rest if all volumes
      // are clear; no need to do all this work for nothing
      if (ch_output_any (m_choffs) == 0) {
        return;
      }

      // OPM/OPN offer 8 different connection algorithms for 4 operators,
      // and OPL3 offers 4 more, which we designate here as 8-11.
      //
      // The operators are computed in order, with the inputs pulled from
      // an array of values (opout) that is populated as we go:
      //    0 = 0
      //    1 = O1
      //    2 = O2
      //    3 = O3
      //    4 = (O4)
      //    5 = O1+O2
      //    6 = O1+O3
      //    7 = O2+O3
      //
      // The s_algorithm_ops table describes the inputs and outputs of each
      // algorithm as follows:
      //
      //      ---------x use opout[x] as operator 2 input
      //      ------xxx- use opout[x] as operator 3 input
      //      ---xxx---- use opout[x] as operator 4 input
      //      --x------- include opout[1] in final sum
      //      -x-------- include opout[2] in final sum
      //      x--------- include opout[3] in final sum
      /*uint32*/ int algorithm_ops = s_algorithm_ops[ch_algorithm (m_choffs)];

      // populate the opout table
      /*sint16*/ int[] opout = new int[8];
      opout[0] = sint16 (0);
      opout[1] = sint16 (op1value);

      // compute the 14-bit volume/value of operator 2
      opmod = opout[bitfield (algorithm_ops, 0, 1)] >> 1;
      opout[2] = sint16 (m_op[1].compute_volume (m_op[1].phase () + opmod, am_offset));
      opout[5] = sint16 (opout[1] + opout[2]);

      // compute the 14-bit volume/value of operator 3
      opmod = opout[bitfield (algorithm_ops, 1, 3)] >> 1;
      opout[3] = sint16 (m_op[2].compute_volume (m_op[2].phase () + opmod, am_offset));
      opout[6] = sint16 (opout[1] + opout[3]);
      opout[7] = sint16 (opout[2] + opout[3]);

      // compute the 14-bit volume/value of operator 4; this could be a noise
      // value on the OPM; all algorithms consume OP4 output at a minimum
      /*sint32*/ int result;
      if (noise_enable () != 0 && m_choffs == 7) {
        result = m_op[3].compute_noise_volume (am_offset);
      } else {
        opmod = opout[bitfield (algorithm_ops, 4, 3)] >> 1;
        result = m_op[3].compute_volume (m_op[3].phase () + opmod, am_offset);
      }
      result >>= rshift;

      // optionally add OP1, OP2, OP3
      /*sint32*/ int clipmin = -clipmax - 1;
      if (bitfield (algorithm_ops, 7) != 0) {
        result = clamp (result + (opout[1] >> rshift), clipmin, clipmax);
      }
      if (bitfield (algorithm_ops, 8) != 0) {
        result = clamp (result + (opout[2] >> rshift), clipmin, clipmax);
      }
      if (bitfield (algorithm_ops, 9) != 0) {
        result = clamp (result + (opout[3] >> rshift), clipmin, clipmax);
      }

      // add to the output
      if (ch_output_0 (m_choffs) != 0) {
        buffer[pointer    ] += result;
      }
      if (ch_output_1 (m_choffs) != 0) {
        buffer[pointer + 1] += result;
      }
    }

  }  //class fm_channel



  //ym2151
  // internal state
  private /*uint8*/ int m_address;  // address register

  // fm_engine_base represents a set of operators and channels which together
  // form a Yamaha FM core; chips that implement other engines (ADPCM, wavetable,
  // etc) take this output and combine it with the others externally
  //fm_engine_base implements ymfm_engine_callbacks

  //fm_engine_base
  // internal state
  //private ymfm_interface m_intf;  // reference to the system interface
  private /*uint32*/ int m_env_counter;  // envelope counter; low 2 bits are sub-counter
  private /*uint8*/ int m_status;  // current status register
  private /*uint8*/ int m_clock_prescale;  // prescale factor (2/3/6)
  private /*uint8*/ int m_irq_mask;  // mask of which bits signal IRQs
  private /*uint8*/ boolean m_irq_state;  // current IRQ state
  private /*uint8*/ boolean[] m_timer_running;  // current timer running state
  private /*uint8*/ int m_total_clocks;  // low 8 bits of the total number of clocks processed
  private /*uint32*/ int m_active_channels;  // mask of active channels (computed by prepare)
  private /*uint32*/ int m_modified_channels;  // mask of channels that have been modified
  private /*uint32*/ int m_prepare_count;  // counter to do periodic prepare sweeps
  public fm_channel[] m_channel = new fm_channel[CHANNELS];  // channel pointers
  private fm_operator[] m_operator = new fm_operator[OPERATORS];  // operator pointers

  //opm_registers
  // OPM register map:
  //
  //      System-wide registers:
  //           01 xxxxxx-x Test register
  //              ------x- LFO reset
  //           08 -x------ Key on/off operator 4
  //              --x----- Key on/off operator 3
  //              ---x---- Key on/off operator 2
  //              ----x--- Key on/off operator 1
  //              -----xxx Channel select
  //           0F x------- Noise enable
  //              ---xxxxx Noise frequency
  //           10 xxxxxxxx Timer A value (upper 8 bits)
  //           11 ------xx Timer A value (lower 2 bits)
  //           12 xxxxxxxx Timer B value
  //           14 x------- CSM mode
  //              --x----- Reset timer B
  //              ---x---- Reset timer A
  //              ----x--- Enable timer B
  //              -----x-- Enable timer A
  //              ------x- Load timer B
  //              -------x Load timer A
  //           18 xxxxxxxx LFO frequency
  //           19 0xxxxxxx AM LFO depth
  //              1xxxxxxx PM LFO depth
  //           1B xx------ CT (2 output data lines)
  //              ------xx LFO waveform
  //
  //     Per-channel registers (channel in address bits 0-2)
  //        20-27 x------- Pan right
  //              -x------ Pan left
  //              --xxx--- Feedback level for operator 1 (0-7)
  //              -----xxx Operator connection algorithm (0-7)
  //        28-2F -xxxxxxx Key code
  //        30-37 xxxxxx-- Key fraction
  //        38-3F -xxx---- LFO PM sensitivity
  //              ------xx LFO AM shift
  //
  //     Per-operator registers (channel in address bits 0-2, operator in bits 3-4)
  //        40-5F -xxx---- Detune value (0-7)
  //              ----xxxx Multiple value (0-15)
  //        60-7F -xxxxxxx Total level (0-127)
  //        80-9F xx------ Key scale rate (0-3)
  //              ---xxxxx Attack rate (0-31)
  //        A0-BF x------- LFO AM enable
  //              ---xxxxx Decay rate (0-31)
  //        C0-DF xx------ Detune 2 value (0-3)
  //              ---xxxxx Sustain rate (0-31)
  //        E0-FF xxxx---- Sustain level (0-15)
  //              ----xxxx Release rate (0-15)
  //
  //     Internal (fake) registers:
  //           1A -xxxxxxx PM depth
  // internal state
  protected /*uint32*/ int m_lfo_counter;  // LFO counter
  protected /*uint32*/ int m_noise_lfsr;  // noise LFSR state
  protected /*uint8*/ int m_noise_counter;  // noise counter
  protected /*uint8*/ int m_noise_state;  // latched noise state
  protected /*uint8*/ int m_noise_lfo;  // latched LFO noise value
  protected /*uint8*/ int m_lfo_am;  // current LFO AM value
  protected /*uint8*/ int[] m_regdata;  // register data
  protected /*sint16*/ int[] m_lfo_waveform;  // LFO waveforms; AM in low 8, PM in upper 8
  protected /*uint16*/ int[] m_waveform;  // waveforms



  public YM2151 () {

    //-------------------------------------------------
    //  ym2151 - constructor
    //-------------------------------------------------
    m_address = uint8 (0);

    //-------------------------------------------------
    //  fm_engine_base - constructor
    //-------------------------------------------------
    //m_intf = intf;
    m_env_counter = 0;
    m_status = uint8 (0);
    m_clock_prescale = uint8 (DEFAULT_PRESCALE);
    m_irq_mask = uint8 (STATUS_TIMERA | STATUS_TIMERB);
    m_irq_state = false;
    m_timer_running = new boolean[] { false, false };
    m_active_channels = ALL_CHANNELS;
    m_modified_channels = ALL_CHANNELS;
    m_prepare_count = 0;

    // inform the interface of their engine
    //m_intf.m_engine = this;

    // create the channels
    for (/*uint32*/ int chnum = 0; chnum < CHANNELS; chnum++) {
      m_channel[chnum] = new fm_channel (channel_offset (chnum));
    }

    // create the operators
    for (/*uint32*/ int opnum = 0; opnum < OPERATORS; opnum++) {
      m_operator[opnum] = new fm_operator (operator_offset (opnum));
    }

    // do the initial operator assignment
    //-------------------------------------------------
    //  assign_operators - get the current mapping of
    //  operators to channels and assign them all
    //-------------------------------------------------
    //fm_engine_base::assign_operators
    for (/*uint32*/ int chnum = 0; chnum < CHANNELS; chnum++) {
      for (/*uint32*/ int index = 0; index < 4; index++) {
        /*uint32*/ int opnum = bitfield (s_fixed_map[chnum], 8 * index, 8);
        m_channel[chnum].assign (index, m_operator[opnum]);
      }
    }

    //-------------------------------------------------
    //  opm_registers - constructor
    //-------------------------------------------------
    m_lfo_counter = 0;
    m_noise_lfsr = 1;
    m_noise_counter = uint8 (0);
    m_noise_state = uint8 (0);
    m_noise_lfo = uint8 (0);
    m_lfo_am = uint8 (0);

    m_regdata = new /*uint8*/ int[REGISTERS];
    m_lfo_waveform = new /*sint16*/ int[LFO_WAVEFORM_LENGTH * 4];
    m_waveform = new /*uint16*/ int[WAVEFORM_LENGTH * WAVEFORMS];

    // create the waveforms
    for (/*uint32*/ int index = 0; index < WAVEFORM_LENGTH; index++) {
      m_waveform[WAVEFORM_LENGTH * 0 + index] = uint16 (abs_sin_attenuation (index) | (bitfield (index, 9) << 15));
    }

    // create the LFO waveforms; AM in the low 8 bits, PM in the upper 8
    // waveforms are adjusted to match the pictures in the application manual
    for (/*uint32*/ int index = 0; index < LFO_WAVEFORM_LENGTH; index++) {
      // waveform 0 is a sawtooth
      /*uint8*/ int am = uint8 (index ^ 0xff);
      /*sint8*/ int pm = sint8 (index);
      m_lfo_waveform[LFO_WAVEFORM_LENGTH * 0 + index] = sint16 (am | (pm << 8));

      // waveform 1 is a square wave
      am = uint8 (bitfield (index, 7) != 0 ? 0 : 0xff);
      pm = sint8 (am ^ 0x80);
      m_lfo_waveform[LFO_WAVEFORM_LENGTH * 1 + index] = sint16 (am | (pm << 8));

      // waveform 2 is a triangle wave
      am = uint8 (bitfield (index, 7) != 0 ? (index << 1) : ((index ^ 0xff) << 1));
      pm = sint8 (bitfield (index, 6) != 0 ? am : ~am);
      m_lfo_waveform[LFO_WAVEFORM_LENGTH * 2 + index] = sint16 (am | (pm << 8));

      // waveform 3 is noise; it is filled in dynamically
      m_lfo_waveform[LFO_WAVEFORM_LENGTH * 3 + index] = sint16 (0);
    }

    init2 ();
  }

  // reset
  //-------------------------------------------------
  //  reset - reset the system
  //-------------------------------------------------
  // reset the overall state
  //-------------------------------------------------
  //  reset - reset the overall state
  //-------------------------------------------------
  public void reset () {
    // reset the engines
    // reset all status bits
    set_reset_status (0, 0xff);

    // register type-specific initialization
    // reset to initial state
    //-------------------------------------------------
    //  reset - reset to initial state
    //-------------------------------------------------
    Arrays.fill (m_regdata, 0, REGISTERS, uint8 (0));

    // enable output on both channels by default
    m_regdata[0x20] = m_regdata[0x21] = m_regdata[0x22] = m_regdata[0x23] = uint8 (0xc0);
    m_regdata[0x24] = m_regdata[0x25] = m_regdata[0x26] = m_regdata[0x27] = uint8 (0xc0);

    // explicitly write to the mode register since it has side-effects
    // QUESTION: old cores initialize this to 0x30 -- who is right?
    writeAddress (REG_MODE);
    writeData (0);

    // reset the channels
    for (fm_channel chan : m_channel) {
      chan.reset ();
    }

    // reset the operators
    for (fm_operator op : m_operator) {
      op.reset ();
    }
  }

  //-------------------------------------------------
  //  read_status - read the status register
  //-------------------------------------------------
  //public /*uint8*/ int read_status () {
  public int readStatus () {
    /*uint8*/ int result = uint8 (status ());
    //if (m_intf.ymfm_is_busy ()) {
    if (listener != null && listener.isBusy ()) {
      //result |= STATUS_BUSY;
      result = uint8 (result | STATUS_BUSY);
    }
    return uint8 (result);
  }

  //-------------------------------------------------
  //  writeAddress - handle a write to the address
  //  register
  //-------------------------------------------------
  public void writeAddress (int address) {
    address &= 0xff;

    // just set the address
    m_address = address;
  }

  //-------------------------------------------------
  //  write - handle a write to the register
  //  interface
  //-------------------------------------------------
  public void writeData (int data) {
    data &= 0xff;
    if (listener != null) {
      listener.written (pointer, m_address, data);
    }

    // write the FM register
    // special case: writes to the mode register can impact IRQs;
    // schedule these writes to ensure ordering with timers
    if (m_address == REG_MODE) {
      //m_intf.ymfm_sync_mode_write (data);
      engine_mode_write (data);
      return;
    }

    // for now just mark all channels as modified
    m_modified_channels = ALL_CHANNELS;

    // most writes are passive, consumed only when needed
    regs_write (m_address, data);
    // handle writes to the key on index
    if (m_address == 0x08) {
      /*uint32*/ int keyon_channel = bitfield (data, 0, 3);
      /*uint32*/ int keyon_opmask = bitfield (data, 3, 4);
      // handle writes to the keyon register(s)
      if (keyon_channel < CHANNELS) {
        // normal channel on/off
        m_channel[keyon_channel].keyonoff (keyon_opmask, KEYON_NORMAL, keyon_channel);
      }
    }

    // special cases
    if (m_address == 0x1b) {
      // writes to register 0x1B send the upper 2 bits to the output lines
      //m_intf.ymfm_external_write (0, data >>> 6);
      if (listener != null) {
        listener.control ((data >>> 6) & 3);
      }
    }

    // mark busy for a bit
    //m_intf.ymfm_set_busy_end (32 * clock_prescale ());
    listener.busy (32 * clock_prescale ());
  }

  // generate one sample of sound
  //-------------------------------------------------
  //  generate - generate one sample of sound
  //-------------------------------------------------
  //ym2151::generate
  public void generate (int limit) {
    for (; pointer < limit; pointer += 2) {

      // clock the system

      //-------------------------------------------------
      //  clock - iterate over all channels, clocking
      //  them forward one step
      //-------------------------------------------------
      //fm_engine_base::clock

      // update the clock counter
      //m_total_clocks++;
      m_total_clocks = uint8 (m_total_clocks + 1);

      // if something was modified, prepare
      // also prepare every 4k samples to catch ending notes
      if (m_modified_channels != 0 || Integer.compareUnsigned (m_prepare_count++, 4096) >= 0) {

        // call each channel to prepare
        m_active_channels = 0;
        for (/*uint32*/ int chnum = 0; chnum < CHANNELS; chnum++) {
          if (m_channel[chnum].prepare ()) {
            m_active_channels |= 1 << chnum;
          }
        }

        // reset the modified channels and prepare count
        m_modified_channels = m_prepare_count = 0;
      }

      // if the envelope clock divider is 1, just increment by 4;
      // otherwise, increment by 1 and manually wrap when we reach the divide count
      if (EG_CLOCK_DIVIDER == 1) {
        m_env_counter += 4;
      } else if (bitfield (++m_env_counter, 0, 2) == EG_CLOCK_DIVIDER) {
        m_env_counter += 4 - EG_CLOCK_DIVIDER;
      }

      // clock the noise generator
      /*sint32*/ int lfo_raw_pm = clock_noise_and_lfo ();

      // now update the state of all the channels and operators
      for (/*uint32*/ int chnum = 0; chnum < CHANNELS; chnum++) {
        m_channel[chnum].clock (m_env_counter, lfo_raw_pm);
      }

      //ym2151::generate

      // update the FM content; OPM is full 14-bit with no intermediate clipping
      buffer[pointer    ] = 0;
      buffer[pointer + 1] = 0;
      /*uint32*/ int chanmask = channelMask;  //ALL_CHANNELS

      //fm_engine_base::output

      // mask out inactive channels
      chanmask &= m_active_channels;

      // sum over all the desired channels
      for (/*uint32*/ int chnum = 0; chnum < CHANNELS; chnum++) {
        if (bitfield (chanmask, chnum) != 0) {
          m_channel[chnum].output_4op (0, 32767);
        }
      }

      //ym2151::generate

      // YM2151 uses an external DAC (YM3012) with mantissa/exponent format
      // convert to 10.3 floating point value and back to simulate truncation
      buffer[pointer    ] = roundtrip_fp (buffer[pointer    ]);
      buffer[pointer + 1] = roundtrip_fp (buffer[pointer + 1]);

    }  //for pointer
  }



  //class fm_engine_base

  // return the current status
  //-------------------------------------------------
  //  status - return the current state of the
  //  status flags
  //-------------------------------------------------
  public /*uint8*/ int status () {
    return uint8 (m_status & ~STATUS_BUSY);
  }

  // set/reset bits in the status register, updating the IRQ status
  public /*uint8*/ int set_reset_status (/*uint8*/ int set, /*uint8*/ int reset) {
    m_status = uint8 ((m_status | set) & ~(reset | STATUS_BUSY));
    //m_intf.ymfm_sync_check_interrupts ();
    engine_check_interrupts ();
    return uint8 (m_status);
  }

  // set the IRQ mask
  public void set_irq_mask (/*uint8*/ int mask) {
    m_irq_mask = uint8 (mask);
    //m_intf.ymfm_sync_check_interrupts ();
    engine_check_interrupts ();
  }

  // return the current clock prescale
  public /*uint32*/ int clock_prescale () {
    return m_clock_prescale;
  }

  // set prescale factor (2/3/6)
  public void set_clock_prescale (/*uint32*/ int prescale) {
    m_clock_prescale = uint8 (prescale);
  }

  // timer callback; called by the interface when a timer fires
  //-------------------------------------------------
  //  engine_timer_expired - timer has expired - signal
  //  status and possibly IRQs
  //-------------------------------------------------
  public void engine_timer_expired (/*uint32*/ int tnum) {
    // update status
    if (tnum == 0 && enable_timer_a () != 0) {
      set_reset_status (STATUS_TIMERA, 0);
    } else if (tnum == 1 && enable_timer_b () != 0) {
      set_reset_status (STATUS_TIMERB, 0);
    }

    // if timer A fired in CSM mode, trigger CSM on all relevant channels
    if (tnum == 0 && csm () != 0) {
      for (/*uint32*/ int chnum = 0; chnum < CHANNELS; chnum++) {
        if (bitfield (CSM_TRIGGER_MASK, chnum) != 0) {
          m_channel[chnum].keyonoff (1, KEYON_CSM, chnum);
          m_modified_channels |= 1 << chnum;
        }
      }
    }

    // reset
    m_timer_running[tnum] = false;
    update_timer (tnum, 1, 0);
  }

  // check interrupts; called by the interface after synchronization
  //-------------------------------------------------
  //  check_interrupts - check the interrupt sources
  //  for interrupts
  //-------------------------------------------------
  public void engine_check_interrupts () {
    // update the state
    /*uint8*/ boolean old_state = m_irq_state;
    m_irq_state = (m_status & m_irq_mask) != 0;

    // set the IRQ status bit
    if (m_irq_state) {
      //m_status |= STATUS_IRQ;
      m_status = uint8 (m_status | STATUS_IRQ);
    } else {
      //m_status &= ~STATUS_IRQ;
      m_status = uint8 (m_status & (~STATUS_IRQ));
    }

    // if changed, signal the new state
    if (old_state != m_irq_state) {
      //m_intf.ymfm_update_irq (m_irq_state);
      listener.irq (m_irq_state);
    }
  }

  // mode register write; called by the interface after synchronization
  //-------------------------------------------------
  //  engine_mode_write - handle a mode register write
  //  via timer callback
  //-------------------------------------------------
  public void engine_mode_write (/*uint8*/ int data) {
    // mark all channels as modified
    m_modified_channels = ALL_CHANNELS;

    // actually write the mode register now
    regs_write (REG_MODE, data);

    // reset IRQ status -- when written, all other bits are ignored
    // QUESTION: should this maybe just reset the IRQ bit and not all the bits?
    //   That is, check_interrupts would only set, this would only clear?

    // reset timer status
    /*uint8*/ int reset_mask = 0;
    if (reset_timer_b () != 0) {
      //reset_mask |= STATUS_TIMERB;
      reset_mask = uint8 (reset_mask | STATUS_TIMERB);
    }
    if (reset_timer_a () != 0) {
      //reset_mask |= STATUS_TIMERA;
      reset_mask = uint8 (reset_mask | STATUS_TIMERA);
    }
    set_reset_status (0, reset_mask);

    // load timers; note that timer B gets a small negative adjustment because
    // the *16 multiplier is free-running, so the first tick of the clock
    // is a bit shorter
    updateTimerB (load_timer_b (), -(m_total_clocks & 15));
    updateTimerA (load_timer_a (), 0);
  }

  protected void updateTimerA (int enable, int delta_clocks) {
    update_timer (0, enable, delta_clocks);
  }
  protected void updateTimerB (int enable, int delta_clocks) {
    update_timer (1, enable, delta_clocks);
  }

  // update the state of the given timer
  //-------------------------------------------------
  //  update_timer - update the state of the given
  //  timer
  //-------------------------------------------------
  protected void update_timer (/*uint32*/ int tnum, /*uint32*/ int enable, /*sint32*/ int delta_clocks) {
    // if the timer is live, but not currently enabled, set the timer
    if (enable != 0 && !m_timer_running[tnum]) {
      // period comes from the registers, and is different for each
      /*uint32*/ int period = (tnum == 0) ? (1024 - timer_a_value ()) : 16 * (256 - timer_b_value ());

      // caller can also specify a delta to account for other effects
      period += delta_clocks;

      // reset it
      //m_intf.ymfm_set_timer (tnum, period * OPERATORS * m_clock_prescale);
      if (tnum == 0) {
        listener.timerA (period * OPERATORS * m_clock_prescale);
      } else {
        listener.timerB (period * OPERATORS * m_clock_prescale);
      }
      m_timer_running[tnum] = true;
    }

    // if the timer is not live, ensure it is not enabled
    else if (enable == 0) {
      //m_intf.ymfm_set_timer (tnum, -1);
      if (tnum == 0) {
        listener.timerA (-1);
      } else {
        listener.timerB (-1);
      }
      m_timer_running[tnum] = false;
    }
  }



  //class opm_registers

  // map channel number to register offset
  public static /*uint32*/ int channel_offset (/*uint32*/ int chnum) {
    return chnum;
  }

  // map operator number to register offset
  public static /*uint32*/ int operator_offset (/*uint32*/ int opnum) {
    return opnum;
  }

  // handle writes to the register array
  //-------------------------------------------------
  //  write - handle writes to the register array
  //-------------------------------------------------
  public void regs_write (/*uint16*/ int index, /*uint8*/ int data) {
    // LFO AM/PM depth are written to the same register (0x19);
    // redirect the PM depth to an unused neighbor (0x1a)
    if (index == 0x19) {
      m_regdata[index + bitfield (data, 7)] = uint8 (data);
    } else if (index != 0x1a) {
      m_regdata[index] = uint8 (data);
    }
  }

  // clock the noise and LFO, if present, returning LFO PM value
  //-------------------------------------------------
  //  clock_noise_and_lfo - clock the noise and LFO,
  //  handling clock division, depth, and waveform
  //  computations
  //-------------------------------------------------
  public int clock_noise_and_lfo () {
    // base noise frequency is measured at 2x 1/2 FM frequency; this
    // means each tick counts as two steps against the noise counter
    /*uint32*/ int freq = noise_frequency ();
    for (int rep = 0; rep < 2; rep++) {
      // evidence seems to suggest the LFSR is clocked continually and just
      // sampled at the noise frequency for output purposes; note that the
      // low 8 bits are the most recent 8 bits of history while bits 8-24
      // contain the 17 bit LFSR state
      m_noise_lfsr <<= 1;
      m_noise_lfsr |= bitfield (m_noise_lfsr, 17) ^ bitfield (m_noise_lfsr, 14) ^ 1;

      // compare against the frequency and latch when we exceed it
      //if (m_noise_counter++ >= freq) {
      if (Integer.compareUnsigned ((m_noise_counter = uint8 (m_noise_counter + 1)), freq) >= 0) {
        m_noise_counter = uint8 (0);
        m_noise_state = uint8 (bitfield (m_noise_lfsr, 17));
      }
    }

    // treat the rate as a 4.4 floating-point step value with implied
    // leading 1; this matches exactly the frequencies in the application
    // manual, though it might not be implemented exactly this way on chip
    /*uint32*/ int rate = lfo_rate ();
    m_lfo_counter += (0x10 | bitfield (rate, 0, 4)) << bitfield (rate, 4, 4);

    // bit 1 of the test register is officially undocumented but has been
    // discovered to hold the LFO in reset while active
    if (lfo_reset () != 0) {
      m_lfo_counter = 0;
    }

    // now pull out the non-fractional LFO value
    /*uint32*/ int lfo = bitfield (m_lfo_counter, 22, 8);

    // fill in the noise entry 1 ahead of our current position; this
    // ensures the current value remains stable for a full LFO clock
    // and effectively latches the running value when the LFO advances
    /*uint32*/ int lfo_noise = bitfield (m_noise_lfsr, 17, 8);
    m_lfo_waveform[LFO_WAVEFORM_LENGTH * 3 + ((lfo + 1) & 0xff)] = sint16 (lfo_noise | (lfo_noise << 8));

    // fetch the AM/PM values based on the waveform; AM is unsigned and
    // encoded in the low 8 bits, while PM signed and encoded in the upper
    // 8 bits
    /*sint32*/ int ampm = m_lfo_waveform[LFO_WAVEFORM_LENGTH * lfo_waveform () + lfo];

    // apply depth to the AM value and store for later
    m_lfo_am = uint8 (((ampm & 0xff) * lfo_am_depth ()) >>> 7);

    // apply depth to the PM value and return it
    return ((ampm >> 8) * lfo_pm_depth ()) >> 7;
  }

  // return the AM offset from LFO for the given channel
  //-------------------------------------------------
  //  lfo_am_offset - return the AM offset from LFO
  //  for the given channel
  //-------------------------------------------------
  public /*uint32*/ int lfo_am_offset (/*uint32*/ int choffs) {
    // OPM maps AM quite differently from OPN

    // shift value for AM sensitivity is [*, 0, 1, 2],
    // mapping to values of [0, 23.9, 47.8, and 95.6dB]
    /*uint32*/ int am_sensitivity = ch_lfo_am_sens (choffs);
    if (am_sensitivity == 0) {
      return 0;
    }

    // QUESTION: see OPN note below for the dB range mapping; it applies
    // here as well

    // raw LFO AM value on OPM is 0-FF, which is already a factor of 2
    // larger than the OPN below, putting our staring point at 2x theirs;
    // this works out since our minimum is 2x their maximum
    return m_lfo_am << (am_sensitivity - 1);
  }

  // return the current noise state, gated by the noise clock
  public /*uint32*/ int noise_state () {
    return m_noise_state;
  }

  // caching helpers
  //-------------------------------------------------
  //  cache_operator_data - fill the operator cache
  //  with prefetched data
  //-------------------------------------------------
  public void cache_operator_data (/*uint32*/ int choffs, /*uint32*/ int opoffs, opdata_cache cache) {
    // set up the easy stuff
    cache.waveform = m_waveform;

    // get frequency from the channel
    /*uint32*/ int block_freq = cache.block_freq = ch_block_freq (choffs);

    // compute the keycode: block_freq is:
    //
    //     BBBCCCCFFFFFF
    //     ^^^^^
    //
    // the 5-bit keycode is just the top 5 bits (block + top 2 bits
    // of the key code)
    /*uint32*/ int keycode = bitfield (block_freq, 8, 5);

    // detune adjustment
    cache.detune = detune_adjustment (op_detune (opoffs), keycode);

    // multiple value, as an x.1 value (0 means 0.5)
    cache.multiple = op_multiple (opoffs) * 2;
    if (cache.multiple == 0) {
      cache.multiple = 1;
    }

    // phase step, or PHASE_STEP_DYNAMIC if PM is active; this depends on
    // block_freq, detune, and multiple, so compute it after we've done those
    if (lfo_pm_depth () == 0 || ch_lfo_pm_sens (choffs) == 0){
      cache.phase_step = compute_phase_step (choffs, opoffs, cache, 0);
    } else {
      cache.phase_step = PHASE_STEP_DYNAMIC;
    }

    // total level, scaled by 8
    cache.total_level = op_total_level (opoffs) << 3;

    // 4-bit sustain level, but 15 means 31 so effectively 5 bits
    cache.eg_sustain = op_sustain_level (opoffs);
    cache.eg_sustain |= (cache.eg_sustain + 1) & 0x10;
    cache.eg_sustain <<= 5;

    // determine KSR adjustment for enevlope rates
    /*uint32*/ int ksrval = keycode >>> (op_ksr (opoffs) ^ 3);
    cache.eg_rate[EG_ATTACK] = uint8 (effective_rate (op_attack_rate (opoffs) * 2, ksrval));
    cache.eg_rate[EG_DECAY] = uint8 (effective_rate (op_decay_rate (opoffs) * 2, ksrval));
    cache.eg_rate[EG_SUSTAIN] = uint8 (effective_rate (op_sustain_rate (opoffs) * 2, ksrval));
    cache.eg_rate[EG_RELEASE] = uint8 (effective_rate (op_release_rate (opoffs) * 4 + 2, ksrval));
  }

  // compute the phase step, given a PM value
  //-------------------------------------------------
  //  compute_phase_step - compute the phase step
  //-------------------------------------------------
  public /*uint32*/ int compute_phase_step (/*uint32*/ int choffs, /*uint32*/ int opoffs, opdata_cache cache, /*sint32*/ int lfo_raw_pm) {
    // OPM logic is rather unique here, due to extra detune
    // and the use of key codes (not to be confused with keycode)

    // start with coarse detune delta; table uses cents value from
    // manual, converted into 1/64ths
    /*sint32*/ int delta = s_detune2_delta[op_detune2 (opoffs)];

    // add in the PM delta
    /*uint32*/ int pm_sensitivity = ch_lfo_pm_sens (choffs);
    if (pm_sensitivity != 0) {
      // raw PM value is -127..128 which is +/- 200 cents
      // manual gives these magnitudes in cents:
      //    0, +/-5, +/-10, +/-20, +/-50, +/-100, +/-400, +/-700
      // this roughly corresponds to shifting the 200-cent value:
      //    0  >> 5,  >> 4,  >> 3,  >> 2,  >> 1,   << 1,   << 2
      if (pm_sensitivity < 6) {
        delta += lfo_raw_pm >> (6 - pm_sensitivity);
      } else {
        delta += lfo_raw_pm << (pm_sensitivity - 5);
      }
    }

    // apply delta and convert to a frequency number
    /*uint32*/ int phase_step = opm_key_code_to_phase_step (cache.block_freq, delta);

    // apply detune based on the keycode
    phase_step += cache.detune;

    // apply frequency multiplier (which is cached as an x.1 value)
    return (phase_step * cache.multiple) >>> 1;
  }

  // return a bitfield extracted from a byte
  private /*uint32*/ int regbyte (/*uint32*/ int offset, /*uint32*/ int start, /*uint32*/ int count) {
    return bitfield (m_regdata[offset + 0], start, count);
  }
  private /*uint32*/ int regbyte (/*uint32*/ int offset, /*uint32*/ int start, /*uint32*/ int count, /*uint32*/ int extra_offset) {
    return bitfield (m_regdata[offset + extra_offset], start, count);
  }

  // return a bitfield extracted from a pair of bytes, MSBs listed first
  private /*uint32*/ int regword (/*uint32*/ int offset1, /*uint32*/ int start1, /*uint32*/ int count1, /*uint32*/ int offset2, /*uint32*/ int start2, /*uint32*/ int count2) {
    return (regbyte (offset1, start1, count1, 0) << count2) | regbyte (offset2, start2, count2, 0);
  }
  private /*uint32*/ int regword (/*uint32*/ int offset1, /*uint32*/ int start1, /*uint32*/ int count1, /*uint32*/ int offset2, /*uint32*/ int start2, /*uint32*/ int count2, /*uint32*/ int extra_offset) {
    return (regbyte (offset1, start1, count1, extra_offset) << count2) | regbyte (offset2, start2, count2, extra_offset);
  }

  // system-wide registers
  public /*uint32*/ int test () {
    return regbyte (0x01, 0, 8);
  }
  public /*uint32*/ int lfo_reset () {
    return regbyte (0x01, 1, 1);
  }
  public /*uint32*/ int noise_frequency () {
    return regbyte (0x0f, 0, 5) ^ 0x1f;
  }
  public /*uint32*/ int noise_enable () {
    return regbyte (0x0f, 7, 1);
  }
  public /*uint32*/ int timer_a_value () {
    return regword (0x10, 0, 8, 0x11, 0, 2);
  }
  public /*uint32*/ int timer_b_value () {
    return regbyte (0x12, 0, 8);
  }
  public /*uint32*/ int csm () {
    return regbyte (0x14, 7, 1);
  }
  public /*uint32*/ int reset_timer_b () {
    return regbyte (0x14, 5, 1);
  }
  public /*uint32*/ int reset_timer_a () {
    return regbyte (0x14, 4, 1);
  }
  public /*uint32*/ int enable_timer_b () {
    return regbyte (0x14, 3, 1);
  }
  public /*uint32*/ int enable_timer_a () {
    return regbyte (0x14, 2, 1);
  }
  public /*uint32*/ int load_timer_b () {
    return regbyte (0x14, 1, 1);
  }
  public /*uint32*/ int load_timer_a () {
    return regbyte (0x14, 0, 1);
  }
  public /*uint32*/ int lfo_rate () {
    return regbyte (0x18, 0, 8);
  }
  public /*uint32*/ int lfo_am_depth () {
    return regbyte (0x19, 0, 7);
  }
  public /*uint32*/ int lfo_pm_depth () {
    return regbyte (0x1a, 0, 7);
  }
  public /*uint32*/ int output_bits () {
    return regbyte (0x1b, 6, 2);
  }
  public /*uint32*/ int lfo_waveform () {
    return regbyte (0x1b, 0, 2);
  }

  // per-channel registers
  public /*uint32*/ int ch_output_any (/*uint32*/ int choffs) {
    return regbyte (0x20, 6, 2, choffs);
  }
  public /*uint32*/ int ch_output_0 (/*uint32*/ int choffs) {
    return regbyte (0x20, 6, 1, choffs);
  }
  public /*uint32*/ int ch_output_1 (/*uint32*/ int choffs) {
    return regbyte (0x20, 7, 1, choffs);
  }
  public /*uint32*/ int ch_feedback (/*uint32*/ int choffs) {
    return regbyte (0x20, 3, 3, choffs);
  }
  public /*uint32*/ int ch_algorithm (/*uint32*/ int choffs) {
    return regbyte (0x20, 0, 3, choffs);
  }
  public /*uint32*/ int ch_block_freq (/*uint32*/ int choffs) {
    return regword (0x28, 0, 7, 0x30, 2, 6, choffs);
  }
  public /*uint32*/ int ch_lfo_pm_sens (/*uint32*/ int choffs) {
    return regbyte (0x38, 4, 3, choffs);
  }
  public /*uint32*/ int ch_lfo_am_sens (/*uint32*/ int choffs) {
    return regbyte (0x38, 0, 2, choffs);
  }

  // per-operator registers
  public /*uint32*/ int op_detune (/*uint32*/ int opoffs) {
    return regbyte (0x40, 4, 3, opoffs);
  }
  public /*uint32*/ int op_multiple (/*uint32*/ int opoffs) {
    return regbyte (0x40, 0, 4, opoffs);
  }
  public /*uint32*/ int op_total_level (/*uint32*/ int opoffs) {
    return regbyte (0x60, 0, 7, opoffs);
  }
  public /*uint32*/ int op_ksr (/*uint32*/ int opoffs) {
    return regbyte (0x80, 6, 2, opoffs);
  }
  public /*uint32*/ int op_attack_rate (/*uint32*/ int opoffs) {
    return regbyte (0x80, 0, 5, opoffs);
  }
  public /*uint32*/ int op_lfo_am_enable (/*uint32*/ int opoffs) {
    return regbyte (0xa0, 7, 1, opoffs);
  }
  public /*uint32*/ int op_decay_rate (/*uint32*/ int opoffs) {
    return regbyte (0xa0, 0, 5, opoffs);
  }
  public /*uint32*/ int op_detune2 (/*uint32*/ int opoffs) {
    return regbyte (0xc0, 6, 2, opoffs);
  }
  public /*uint32*/ int op_sustain_rate (/*uint32*/ int opoffs) {
    return regbyte (0xc0, 0, 5, opoffs);
  }
  public /*uint32*/ int op_sustain_level (/*uint32*/ int opoffs) {
    return regbyte (0xe0, 4, 4, opoffs);
  }
  public /*uint32*/ int op_release_rate (/*uint32*/ int opoffs) {
    return regbyte (0xe0, 0, 4, opoffs);
  }



  // this class represents the interface between the fm_engine and the outside
  // world; it provides hooks for timers, synchronization, and I/O
  //class ymfm_interface
  static interface Listener {

    // the following functions must be implemented by any derived classes; the
    // default implementations are sufficient for some minimal operation, but will
    // likely need to be overridden to integrate with the outside world; they are
    // all prefixed with ymfm_ to reduce the likelihood of namespace collisions

    //
    // timing and synchronizaton
    //

    // the chip implementation calls this when a write happens to the mode
    // register, which could affect timers and interrupts; our responsibility
    // is to ensure the system is up to date before calling the engine's
    // engine_mode_write() method
    //public void ymfm_sync_mode_write (/*uint8*/ int data);

    // the chip implementation calls this when the chip's status has changed,
    // which may affect the interrupt state; our responsibility is to ensure
    // the system is up to date before calling the engine's
    // engine_check_interrupts() method
    //public void ymfm_sync_check_interrupts ();

    // the chip implementation calls this when one of the two internal timers
    // has changed state; our responsibility is to arrange to call the engine's
    // engine_timer_expired() method after the provided number of clocks; if
    // duration_in_clocks is negative, we should cancel any outstanding timers
    //public void ymfm_set_timer (/*uint32*/ int tnum, int duration_in_clocks);

    //timerA (clocks)
    //  タイマーAが始動または停止した
    //  clocks  -1以外  始動。タイマーAがオーバーフローするまでの時間
    //          -1      停止
    public void timerA (int clocks);

    //timerB (clocks)
    //  タイマーBが始動または停止した
    //  clocks  -1以外  始動。タイマーBがオーバーフローするまでの時間
    //          -1      停止
    public void timerB (int clocks);

    // the chip implementation calls this to indicate that the chip should be
    // considered in a busy state until the given number of clocks has passed;
    // our responsibility is to compute and remember the ending time based on
    // the chip's clock for later checking
    //public void ymfm_set_busy_end (/*uint32*/ int clocks);

    //busy (clocks)
    //  BUSYがセットされた
    //  clocks  BUSYがクリアされるまでの時間
    public void busy (int clocks);

    // the chip implementation calls this to see if the chip is still currently
    // is a busy state, as specified by a previous call to ymfm_set_busy_end();
    // our responsibility is to compare the current time against the previously
    // noted busy end time and return true if we haven't yet passed it
    //public boolean ymfm_is_busy ();

    //busy = isBusy ()
    //  busy  true   BUSYがセットされている
    //        false  BUSYがセットされていない
    public boolean isBusy ();

    //
    // I/O functions
    //

    // the chip implementation calls this when the state of the IRQ signal has
    // changed due to a status change; our responsibility is to respond as
    // needed to the change in IRQ state, signaling any consumers
    //public void ymfm_update_irq (boolean asserted);

    //irq (asserted)
    //  IRQが変化した
    //  asserted  false  IRQがネゲートされた
    //            true   IRQがアサートされた
    public void irq (boolean asserted);

    // the chip implementation calls this whenever data is read from outside
    // of the chip; our responsibility is to provide the data requested
    //public /*uint8*/ int ymfm_external_read (/*access_class*/ int type, /*uint32*/ int address);

    // the chip implementation calls this whenever data is written outside
    // of the chip; our responsibility is to pass the written data on to any consumers
    //public void ymfm_external_write (/*uint32*/ int address, /*uint8*/ int data);

    //control (data)
    //  コントロール
    //  data  CT1<<1|CT2
    public void control (int data);

    //written (pointer, address, data)
    //  データレジスタへ書き込まれた
    public void written (int pointer, int address, int data);

  }  //interface Listener



  private Listener listener;  //リスナー
  private int[] buffer;  //バッファ
  private int pointer;  //バッファのポインタ
  private int channelMask;  //チャンネルマスク

  //init2 ()
  private void init2 () {
    listener = null;
    buffer = new int[2 * 62500 * 5];  //5s
    pointer = 0;
    channelMask = ALL_CHANNELS;
  }

  //setListener (listener)
  //  リスナーを設定する
  public void setListener (Listener listener) {
    this.listener = listener;
  }

  //allocate (size)
  //  バッファを確保する
  //  バッファはクリアされる
  //  ポインタは先頭に戻る
  public void allocate (int size) {
    buffer = new int[size];
    Arrays.fill (buffer, 0);
    pointer = 0;
  }

  //clear ()
  //  バッファをクリアする
  //  ポインタは先頭に戻る
  //  チップは変化しない
  public void clear () {
    Arrays.fill (buffer, 0);
    pointer = 0;
  }

  //buffer = getBuffer ()
  //  バッファを返す
  public int[] getBuffer () {
    return buffer;
  }

  //pointer = getPointer ()
  //  ポインタを返す
  public int getPointer () {
    return pointer;
  }

  //reset ()
  //  チップをリセットする
  //  バッファは変化しない
  //public void reset () {
  //}

  //generate (limit)
  //  バッファを構築する
  //public void generate (int limit) {
  //}

  //fill ()
  //  バッファを最後まで構築する
  public void fill () {
    generate (buffer.length);
  }

  //readStatus ()
  //  ステータスレジスタから読み出す
  //public int readStatus () {
  //}

  //writeAddress (address)
  //  アドレスレジスタへ書き込む
  //public void writeAddress (int address) {
  //}

  //writeData (data)
  //  データレジスタへ書き込む
  //public void writeData (int data) {
  //}

  //setChannelMask (mask)
  //  チャンネルマスクを設定する
  public void setChannelMask (int mask) {
    channelMask = mask;
  }

  //timerAExpired ()
  //  タイマーAがオーバーフローした
  public void timerAExpired () {
    engine_timer_expired (0);
  }

  //timerBExpired ()
  //  タイマーBがオーバーフローした
  public void timerBExpired () {
    engine_timer_expired (1);
  }



}  //class YM2151
