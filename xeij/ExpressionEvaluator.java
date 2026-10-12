//========================================================================================
//  ExpressionEvaluator.java
//    en:Expression evaluator
//    ja:式評価
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  内部クラスExpressionElementのインスタンスは共通のレジスタやメモリにアクセスする
//
//  値の型
//    値の型は浮動小数点数と文字列の2種類。数値はすべて浮動小数点数
//    原則として値の型はパーサで確認する。print()などの任意の引数を受け取るものを除いて、エバリュエータは引数の型をチェックしない
//  整数演算
//    以下の演算子は浮動小数点数を符号あり64bit整数に飽和変換してから演算を行う
//      x<<y  x>>y  x>>>y  x&y  x^y  x|y  x<<=y  x>>=y  x>>>=y  x&=y  x^=y  x|=y
//    浮動小数点数から符号あり64bit整数への飽和変換
//      符号あり64bit整数の範囲内の値は小数点以下を切り捨てる
//      符号あり64bit整数の範囲外の値は符号あり64bit整数で表現できる最小の値または最大の値に変換する
//      NaNは-1に変換する
//    シフトカウント
//      シフトカウントは符号あり64bit整数の下位6bitを使用する
//    符号なし右シフトの結果
//      符号なし右シフトの結果も符号あり64bit整数とみなす
//      -1>>>1は2**63-1だが、-1>>>0は2**64-1にならず-1のままである
//    アドレス
//      アドレスは符号あり64bit整数の下位32bitを使用する
//    ファンクションコード
//      ファンクションコードは符号あり64bit整数の下位3bitを使用する
//  右辺のx.bとx.wとx.lとx.q
//    x.bはxを符号あり64bit整数に飽和変換してから下位8bitを符号あり8bit整数とみなして符号拡張する。xがアドレスレジスタの場合も同じ
//    x.wはxを符号あり64bit整数に飽和変換してから下位16bitを符号あり16bit整数とみなして符号拡張する。xがアドレスレジスタの場合も同じ
//    x.lはxを符号あり64bit整数に飽和変換してから下位32bitを符号あり32bit整数とみなして符号拡張する
//    x.qはxを符号あり64bit整数に飽和変換する
//  左辺のr0.bとr0.wとr0.l
//    r0.b=yはr0の下位8bitだけを書き換える。r0がアドレスレジスタの場合も同じ
//    r0.w=yはr0の下位16bitだけを書き換える。r0がアドレスレジスタの場合も同じ
//    r0.l=yはr0の下位32bitすなわち全体を書き換える
//  代入演算子
//    代入演算子は左辺を右辺として返す
//      d0.b=yはyを符号あり64bit整数に飽和変換して下位8bitをd0の下位8bitに代入し、代入した値を符号あり8bit整数とみなして符号拡張して返す
//  複合代入演算子
//    複合代入演算子が返す値は2つの演算子に分けた場合と常に一致する
//      d0.b+=yのd0.bは右辺として読まれて左辺として代入されてから再び右辺として読まれる
//      2回目の読み出しは省略できるが符号拡張は省略できない
//
//  ブレークポイントで使える特殊変数
//    count      命令ブレークポイントの通過回数。変更できる
//    threshold  命令ブレークポイントの閾値。変更できる
//    size       データブレークポイントのサイズ。1=バイト,2=ワード,4=ロング。オペレーションサイズと一致しているとは限らない。変更できない
//    data       データブレークポイントで書き込もうとしている値または読み出した値。変更できる
//    usersuper  0=ユーザモード,1=スーパーバイザモード。変更できない
//    readwrite  0=リード,1=ライト。変更できない
//
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,LinkedList,TimeZone,Timer,TimerTask,TreeMap

public class ExpressionEvaluator extends EFPBox {

  //------------------------------------------------------------------------
  //ヘルプメッセージ
  public static final String[] EVX_HELP_MESSAGE_JA = new String[] {
    ("コマンド\n" +
     "        d<サイズ> <開始アドレス>,<終了アドレス¹>\n" +
     "                                メモリダンプ\n" +
     "        f<サイズ> <開始アドレス>,<終了アドレス¹>,<データ>,…\n" +
     "                                メモリ充填\n" +
     "        g <開始アドレス>        実行\n" +
     "        h                       ヘルプ\n" +
     "        i                       停止\n" +
     "        l <開始アドレス>,<終了アドレス¹>\n" +
     "                                逆アセンブル\n" +
     "        ll                      ラベル一覧\n" +
     "        me<サイズ> <開始アドレス>,<データ>,…\n" +
     "                                メモリ編集\n" +
     "        ms<サイズ> <開始アドレス>,<終了アドレス¹>,<データ>,…\n" +
     "                                メモリ検索\n" +
     "        p <式>,…               計算と表示\n" +
     "        r                       ステップアンティルリターン\n" +
     "        s <回数>                ステップ\n" +
     "        t <回数>                トレース\n" +
     "        tx <回数>               レジスタ表示付きトレース\n" +
     "        txf <回数>              浮動小数点レジスタ表示付きトレース\n" +
     "        x                       レジスタ一覧\n" +
     "        xf                      浮動小数点レジスタ一覧\n" +
     "        <式>                    計算\n" +
     "        <コマンド>;…           逐次実行\n" +
     "        ¹終了アドレスは範囲に含まれる\n"),
    ("サイズ\n" +
     "        b       バイト (8bit)\n" +
     "        w       ワード (16bit)\n" +
     "        l       ロング (32bit)\n" +
     "        q       クワッド (64bit)\n" +
     "        s       単精度 (32bit)\n" +
     "        d       倍精度 (64bit)\n" +
     "        x       拡張精度 (80/96bit)\n" +
     "        t       三倍精度 (96bit)\n" +
     "        p       パック10進数 (96bit)\n"),
    ("浮動小数点数\n" +
     "        1.0e+2                  10進数\n" +
     "        0b1.1001p+6             2進数\n" +
     "        0o1.44p+6               8進数\n" +
     "        0x1.9p+6 $64            16進数\n" +
     "        Infinity NaN            無限大, 非数\n" +
     "数学定数\n" +
     "        Apery Catalan E Eular Pi\n" +
     "文字コード\n" +
     "        'A'\n" +
     "文字列\n" +
     "        \"ABC\"\n"),
    ("汎用レジスタ\n" +
     "        d0 … d7 r0 … r7       データレジスタ\n" +
     "        a0 … a7 r8 … r15 sp   アドレスレジスタ\n" +
     "浮動小数点レジスタ\n" +
     "        fp0 … fp7\n" +
     "制御レジスタ\n" +
     "        pc sr ccr sfc dfc cacr tc itt0 itt1 dtt0 dtt1 buscr\n" +
     "        usp vbr caar ssp msp isp urp srp pcr fpiar fpsr fpcr\n" +
     "変数\n" +
     "        foo                     浮動小数点変数\n" +
     "        foo$                    文字列変数\n" +
     "アドレスとファンクションコード\n" +
     "        <アドレス>              現在のアドレス空間\n" +
     "        <物理アドレス>@0        物理アドレス空間\n" +
     "        <論理アドレス>@1        ユーザデータ空間\n" +
     "        <論理アドレス>@2        ユーザコード空間\n" +
     "        <論理アドレス>@5        スーパーバイザデータ空間\n" +
     "        <論理アドレス>@6        スーパーバイザコード空間\n"),
    ("演算子\n" +
     "        <汎用レジスタ>.<サイズ> 汎用レジスタアクセス\n" +
     "        [<アドレス>].<サイズ>   メモリアクセス\n" +
     "        x.<サイズ>              キャスト\n" +
     "        x(y)                    関数呼び出し\n" +
     "        x++ ++x x-- --x         インクリメント, デクリメント\n" +
     "        +x -x ~x !x             符号, ビットNOT, 論理NOT\n" +
     "        x**y x*y x/y x%y        累乗, 乗除算\n" +
     "        x+y x-y                 加減算, 連結\n" +
     "        x<<y x>>y x>>>y         シフト\n" +
     "        x<y x<=y x>y x>=y       比較\n" +
     "        x==y x!=y               等価\n" +
     "        x&y x^y x|y x&&y x||y   ビットAND, XOR, OR, 論理AND, OR\n" +
     "        x?y:z                   条件\n" +
     "        x=y x**=y x*=y x/=y x%=y x+=y x-=y\n" +
     "        x<<=y x>>=y x>>>=y x&=y x^=y x|=y\n" +
     "                                代入, 複合代入\n" +
     "        x,y                     逐次評価\n"),
    ("関数\n" +
     "        abs acos acosh acot acoth acsc acsch agi agm\n" +
     "        asc asec asech asin asinh atan atan2 atanh\n" +
     "        bin$ cbrt ceil chr$ cmp cmp0 cmp1 cmp1abs cmpabs\n" +
     "        cos cosh cot coth csc csch cub dec deg div2 div3 divpi divrz\n" +
     "        exp exp10 exp2 exp2m1 expm1 floor frac getexp getman\n" +
     "        hex$ ieeerem inc iseven isinf isint isnan isodd isone iszero\n" +
     "        lgamma log log10 log1p log2 max min mul2 mul3 mulpi\n" +
     "        oct$ pow quo rad random rcp rint rmode round rprec\n" +
     "        sec sech sgn sin sinh sqrt squ str$ tan tanh tgamma trunc ulp val\n"),
  };
  public static final String[] EVX_HELP_MESSAGE_EN = new String[] {
    ("command\n" +
     "        d<size> <start-address>,<end-address¹>\n" +
     "                                memory dump\n" +
     "        f<size> <start-address>,<end-address¹>,<data>,…\n" +
     "                                memory fill\n" +
     "        g <start-address>       run\n" +
     "        h                       help\n" +
     "        i                       stop\n" +
     "        l <start-address>,<end-address¹>\n" +
     "                                disassemble\n" +
     "        ll                      label list\n" +
     "        me<size> <start-address>,<data>,…\n" +
     "                                memory edit\n" +
     "        ms<size> <start-address>,<end-address¹>,<data>,…\n" +
     "                                memory search\n" +
     "        p <expression>,…       calculate and print\n" +
     "        r                       step until return\n" +
     "        s <number-of-times>     step\n" +
     "        t <number-of-times>     trace\n" +
     "        tx <number-of-times>    trace with register list\n" +
     "        txf <number-of-times>   trace with floating point register list\n" +
     "        x                       register list\n" +
     "        xf                      floating point register list\n" +
     "        <式>                    calculate\n" +
     "        <コマンド>;…           sequential execution\n" +
     "        ¹the end address is within the range\n"),
    ("size\n" +
     "        b       byte (8bit)\n" +
     "        w       word (16bit)\n" +
     "        l       long (32bit)\n" +
     "        q       quad (64bit)\n" +
     "        s       single-precision (32bit)\n" +
     "        d       double-precision (64bit)\n" +
     "        x       extended-precision (80/96bit)\n" +
     "        t       triple-precision (96bit)\n" +
     "        p       packed decimal (96bit)\n"),
    ("floating point number\n" +
     "        1.0e+2                  decimal number\n" +
     "        0b1.1001p+6             binary number\n" +
     "        0o1.44p+6               octal number\n" +
     "        0x1.9p+6 $64            hexadecimal number\n" +
     "        Infinity NaN            infinity, not a number\n" +
     "mathematical constant\n" +
     "        Apery Catalan E Eular Pi\n" +
     "character code\n" +
     "        'A'\n" +
     "string\n" +
     "        \"ABC\"\n"),
    ("general register\n" +
     "        d0 … d7 r0 … r7       data register\n" +
     "        a0 … a7 r8 … r15 sp   address register\n" +
     "floating point register\n" +
     "        fp0 … fp7\n" +
     "control register\n" +
     "        pc sr ccr sfc dfc cacr tc itt0 itt1 dtt0 dtt1 buscr\n" +
     "        usp vbr caar ssp msp isp urp srp pcr fpiar fpsr fpcr\n" +
     "variable\n" +
     "        foo                     floating point variable\n" +
     "        foo$                    string variable\n" +
     "address space\n" +
     "        <address>               current address space\n" +
     "        <physical-address>@0    physical address space\n" +
     "        <logical-address>@1     user data space\n" +
     "        <logical-address>@2     user code space\n" +
     "        <logical-address>@5     supervisor data space\n" +
     "        <logical-address>@6     supervisor code space\n"),
    ("operator\n" +
     "        <genral-register>.<size>\n" +
     "                                general register access\n" +
     "        [<address>].<size>      memory access\n" +
     "        x.<size>                cast\n" +
     "        x(y)                    function call\n" +
     "        x++ ++x x-- --x         increment, decrement\n" +
     "        +x -x ~x !x             signum, bitwise NOT, logical NOT\n" +
     "        x**y x*y x/y x%y        exponentiation, multiplication and division\n" +
     "        x+y x-y                 addition and subtraction, concatenation\n" +
     "        x<<y x>>y x>>>y         shift\n" +
     "        x<y x<=y x>y x>=y       comparison\n" +
     "        x==y x!=y               equality\n" +
     "        x&y x^y x|y x&&y x||y   bitwise AND, XOR, OR, logical AND, OR\n" +
     "        x?y:z                   conditional\n" +
     "        x=y x**=y x*=y x/=y x%=y x+=y x-=y\n" +
     "        x<<=y x>>=y x>>>=y x&=y x^=y x|=y\n" +
     "                                assignment, compound assignment\n" +
     "        x,y                     sequantial evaluation\n"),
    ("function\n" +
     "        abs acos acosh acot acoth acsc acsch agi agm\n" +
     "        asc asec asech asin asinh atan atan2 atanh\n" +
     "        bin$ cbrt ceil chr$ cmp cmp0 cmp1 cmp1abs cmpabs\n" +
     "        cos cosh cot coth csc csch cub dec deg div2 div3 divpi divrz\n" +
     "        exp exp10 exp2 exp2m1 expm1 floor frac getexp getman\n" +
     "        hex$ ieeerem inc iseven isinf isint isnan isodd isone iszero\n" +
     "        lgamma log log10 log1p log2 max min mul2 mul3 mulpi\n" +
     "        oct$ pow quo rad random rcp rint rmode round rprec\n" +
     "        sec sech sgn sin sinh sqrt squ str$ tan tanh tgamma trunc ulp val\n"),
  };



  //------------------------------------------------------------------------
  //変数
  protected HashMap<String,ExpressionElement> evxVariableMap;  //変数マップ


  //------------------------------------------------------------------------
  //コンストラクタ
  public ExpressionEvaluator () {
    evxVariableMap = new HashMap<String,ExpressionElement> ();
  }



  //========================================================================================
  //評価モード
  //  式評価モード
  //    副作用を伴う演算子(インクリメント、デクリメント、代入、複合代入)を書ける
  //    レジスタは数値
  //  コマンドモード
  //    行頭またはセパレータの直後にコマンドを書ける
  //    副作用を伴う演算子(インクリメント、デクリメント、代入、複合代入)を書ける
  //    レジスタは数値
  //  アセンブラモード
  //    行頭またはセパレータの直後にラベル定義を書ける
  //    行頭またはセパレータの直後またはラベル定義の直後にニモニックを書ける
  //    ニモニックの直後にオペレーションサイズを書ける
  //    副作用を伴う演算子(インクリメント、デクリメント、代入、複合代入)を書けない
  //    レジスタは記号
  protected static final int EVM_EXPRESSION = 1;  //式評価モード
  protected static final int EVM_COMMAND    = 2;  //コマンドモード
  protected static final int EVM_ASSEMBLER  = 3;  //アセンブラモード



  //========================================================================================
  //アセンブル環境
  protected HashMap<Integer,Integer> evxLocalLabelCount;  //number=>count
  protected HashMap<Integer,Integer> evxLocalLabelMap;  //count<<16|number=>address



  //========================================================================================
  //$$EPY 要素の優先順位
  //  ElementPriority
  protected static final int EPY_PRIORITY_PRIMITIVE      = 21;  //基本要素
  protected static final int EPY_PRIORITY_FUNCTION       = 20;  //関数呼び出し      右から
  protected static final int EPY_PRIORITY_AT             = 19;  //＠演算子          左から
  protected static final int EPY_PRIORITY_POSTFIX        = 18;  //後置演算子        左から
  protected static final int EPY_PRIORITY_PREFIX         = 17;  //前置演算子        右から
  protected static final int EPY_PRIORITY_EXPONENTIATION = 16;  //累乗演算子        右から
  protected static final int EPY_PRIORITY_MULTIPLICATION = 15;  //乗除算演算子      左から
  protected static final int EPY_PRIORITY_ADDITION       = 14;  //加減算演算子      左から
  protected static final int EPY_PRIORITY_SHIFT          = 13;  //シフト演算子      左から
  protected static final int EPY_PRIORITY_COMPARISON     = 12;  //比較演算子        左から
  protected static final int EPY_PRIORITY_EQUALITY       = 11;  //等価演算子        左から
  protected static final int EPY_PRIORITY_BITWISE_AND    = 10;  //ビットAND演算子   左から
  protected static final int EPY_PRIORITY_BITWISE_XOR    =  9;  //ビットXOR演算子   左から
  protected static final int EPY_PRIORITY_BITWISE_OR     =  8;  //ビットOR演算子    左から
  protected static final int EPY_PRIORITY_LOGICAL_AND    =  7;  //論理AND演算子     左から
  protected static final int EPY_PRIORITY_LOGICAL_OR     =  6;  //論理OR演算子      左から
  protected static final int EPY_PRIORITY_CONDITIONAL    =  5;  //条件演算子        右から
  protected static final int EPY_PRIORITY_ASSIGNMENT     =  4;  //代入演算子        右から
  protected static final int EPY_PRIORITY_COLON          =  3;  //コロン演算子      左から
  protected static final int EPY_PRIORITY_COMMA          =  2;  //コンマ演算子      左から
  protected static final int EPY_PRIORITY_COMMAND        =  1;  //コマンド、ライン  右から
  protected static final int EPY_PRIORITY_SEPARATOR      =  0;  //セパレータ        左から


  //========================================================================================
  //浮動小数点制御レジスタ
  public static final String[] EVX_FLOAT_CONTROL_NAME_ARRAY = (
    "fpiar," +
    "fpsr," +
    "fpcr," +
    "").split (",");


  //========================================================================================
  //キャッシュ選択
  public static final String[] EVX_CACHE_NAME_ARRAY = (
    "nc," + 
    "dc," + 
    "ic," + 
    "bc," + 
    "").split (",");


  //========================================================================================
  //制御レジスタ
  public static final String[] EVX_CONTROL_CODE_MPU_NAME_ARRAY = (
    "0000 -12346 SFC,"   +  //Source Function Code Register
    "0001 -12346 DFC,"   +  //Destination Function Code Register
    "0002 --2346 CACR,"  +  //Cache Control Register
    "0003 ----46 TC,"    +  //Translation Control Register (TCR)
    "0004 ----46 ITT0,"  +  //Instruction Transparent Translation Register 0
    "0004 ----4- IACR0," +  //Instruction Access Control Register 0
    "0005 ----46 ITT1,"  +  //Instruction Transparent Translation Register 1
    "0005 ----4- IACR1," +  //Instruction Access Control Register 1
    "0006 ----46 DTT0,"  +  //Data Transparent Translation Register 0
    "0006 ----4- DACR0," +  //Data Access Control Register 0
    "0007 ----46 DTT1,"  +  //Data Transparent Translation Register 1
    "0007 ----4- DACR1," +  //Data Access Control Register 1
    "0008 -----6 BUSCR," +  //Bus Control Register
    "0800 -12346 USP,"   +  //User Stack Pointer
    "0801 -12346 VBR,"   +  //Vector Base Register
    "0802 --23-- CAAR,"  +  //Cache Address Register
    "0803 --234- MSP,"   +  //Master Stack Pointer Register
    "0804 --2346 ISP,"   +  //Interrupt Stack Pointer
    "0805 ----4- MMUSR," +  //Memory Management Unit Status Register
    "0806 ----46 URP,"   +  //User Root Pointer
    "0807 ----46 SRP,"   +  //Supervisor Root Pointer
    "0808 -----6 PCR,"   +  //Processor Configuration Register
    "").split (",");
  protected static final HashMap<String,Integer> EVX_CONTROL_NAME_TO_MPU_CODE = new HashMap<String,Integer> ();
  protected static final HashMap<Integer,String> EVX_CONTROL_MPU_CODE_TO_NAME = new HashMap<Integer,String> ();
  static {
    for (String codeMPUName : EVX_CONTROL_CODE_MPU_NAME_ARRAY) {
      int mpuCode = Integer.parseInt (codeMPUName.substring (0, 4), 16);
      for (int i = 5; i <= 10; i++) {
        int c = codeMPUName.charAt (i);
        if (c != '-') {
          mpuCode |= 1 << 16 << (c & 15);
        }
      }
      String name = codeMPUName.substring (12).toLowerCase ();
      EVX_CONTROL_NAME_TO_MPU_CODE.put (name, mpuCode);
      EVX_CONTROL_MPU_CODE_TO_NAME.put (mpuCode, name);
    }
  }



  //========================================================================================
  //$$ETY 要素の型
  //  enum ElementType
  protected enum ElementType {

    //基本要素

    //UNDEF 未定義
    //  関数名や演算子に変換する前のトークンなどの単独では評価できない要素の値の型に使う
    //  パーサが終了した時点で最上位の要素が未定義のときはエラー
    ETY_UNDEF {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("undefined");
      }
    },

    //VOID 値なし
    //  コマンドの返却値
    //  セパレータを除いて引数としては使用できない
    ETY_VOID {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb;
      }
    },

    //VARIABLE_FLOAT
    //  浮動小数点変数
    //  フィールド
    //    exlParamX        変数の本体
    //      exlFloatValue  変数の本体の値
    //    exlStringValue   変数名
    ETY_VARIABLE_FLOAT {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sete (elem.exlParamX.exlFloatValue);  //変数の本体の値
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);  //変数名
      }
    },

    //VARIABLE_STRING
    //  文字列変数
    //  フィールド
    //    exlParamX         変数の本体
    //      exlStringValue  変数の本体の値
    //    exlStringValue   変数名
    ETY_VARIABLE_STRING {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = elem.exlParamX.exlStringValue;  //変数の本体の値
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);  //変数名
      }
    },

    //CONST_FLOAT 浮動小数点数
    //  NaNとInfinityを含む
    //  フィールド
    //    exlFloatValue  数値
    ETY_FLOAT {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlFloatValue.toString ());
      }
    },

    //CONST_STRING 文字列
    //  フィールド
    //    exlStringValue  文字列
    ETY_STRING {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        String str = elem.exlStringValue;
        sb.append ('"');
        for (int i = 0, l = str.length (); i < l; i++) {
          char c = str.charAt (i);
          if (c == '\b') {
            sb.append ("\\b");
          } else if (c == '\f') {
            sb.append ("\\f");
          } else if (c == '\t') {
            sb.append ("\\t");
          } else if (c == '\n') {
            sb.append ("\\n");
          } else if (c == '\r') {
            sb.append ("\\r");
          } else if (0x00 <= c && c <= 0x1f) {
            String.format ("\\x%02x", c);
          } else if (c == '"') {
            sb.append ("\\\"");
          } else if (c == '\\') {
            sb.append ("\\\\");
          } else {
            sb.append (c);
          }
        }
        return sb.append ('"');
      }
    },

    //MATH_* 数学定数
    //  数学的には定数だがInfinityやNaNと違って丸めの影響を受けるので実行時に値が変化する場合がある
    //  フィールド
    //    exlFloatValue  数値
    ETY_MATH_APERY {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setapery ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("Apery");
      }
    },

    ETY_MATH_CATALAN {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setcatalan ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("Catalan");
      }
    },

    ETY_MATH_NAPIER {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setnapier ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("E");
      }
    },

    ETY_MATH_EULER {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seteuler ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("Euler");
      }
    },

    ETY_MATH_PI {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setpi ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("Pi");
      }
    },

    //整数レジスタ
    //  stringValue  レジスタ名
    //    subscript  レジスタ番号
    ETY_INTEGER_REGISTER {  //汎用レジスタ
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (XEiJ.regRn[elem.exlSubscript]);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        if (elem.exlSubscript <= 7) {  // d0～d7
          return sb.append ('d').append (elem.exlSubscript);
        } else if (elem.exlSubscript <= 14) {  // a0～a6
          return sb.append ('a').append (elem.exlSubscript - 8);
        } else {
          return sb.append ("sp");
        }
      }
    },

    //浮動小数点レジスタ
    //  stringValue  レジスタ名
    //    subscript  レジスタ番号
    ETY_FLOATING_POINT_REGISTER {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlSubscript));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("fp").append (elem.exlSubscript);
      }
    },

    //制御レジスタ
    //  stringValue  レジスタ名
    ETY_PC {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlReadPC ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },
    ETY_CCR {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlReadCCR ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },
    ETY_SR {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlReadSR ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },
    ETY_FLOAT_CONTROL_REGISTER {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlReadFloatControlRegister (elem.exlSubscript));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },
    ETY_CONTROL_REGISTER {
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlReadControlRegister (elem.exlSubscript));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },

    //疑似レジスタ

    //サプレスされた整数レジスタ
    //    subscript  レジスタ番号
    //  stringValue  レジスタ名
    ETY_ZERO_REGISTER {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },

    //サプレスされたプログラムカウンタ
    //  stringValue  レジスタ名
    ETY_ZERO_PC {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },

    //オプショナルプログラムカウンタ
    //  stringValue  レジスタ名
    ETY_OPTIONAL_PC {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },

    //キャッシュ選択
    //    subscript   0  1  2  3
    //  stringValue  nc dc ic bc
    ETY_CACHE_SELECTION {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },

    //FUNCTION_* 関数
    //  フィールド
    //    exlValueType    結果の型
    //    exlFloatValue   数値の結果
    //    exlStringValue  文字列の結果
    //    exlParamX       1番目の引数
    //    exlParamY       2番目の引数
    //    exlParamZ       3番目の引数
    ETY_FUNCTION_ABS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.abs (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "abs");
      }
    },

    ETY_FUNCTION_ACOS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.acos (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acos");
      }
    },

    ETY_FUNCTION_ACOSH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.acosh (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acosh");
      }
    },

    ETY_FUNCTION_ACOT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.acot (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acot");
      }
    },

    ETY_FUNCTION_ACOTH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.acoth (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acoth");
      }
    },

    ETY_FUNCTION_ACSC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.acsc (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acsc");
      }
    },

    ETY_FUNCTION_ACSCH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.acsch (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "acsch");
      }
    },

    ETY_FUNCTION_AGI {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.agi (elem.exlParamX.exlEval (mode).exlFloatValue,
                                elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "agi");
      }
    },

    ETY_FUNCTION_AGM {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.agi (elem.exlParamX.exlEval (mode).exlFloatValue,
                                elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "agm");
      }
    },

    ETY_FUNCTION_ASC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlStringValue.charAt (0));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asc");
      }
    },

    ETY_FUNCTION_ASEC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.asec (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asec");
      }
    },

    ETY_FUNCTION_ASECH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.asech (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asech");
      }
    },

    ETY_FUNCTION_ASIN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.asin (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asin");
      }
    },

    ETY_FUNCTION_ASINH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.asinh (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "asinh");
      }
    },

    ETY_FUNCTION_ATAN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.atan (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "atan");
      }
    },

    ETY_FUNCTION_ATAN2 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.atan2 (elem.exlParamX.exlEval (mode).exlFloatValue,
                                  elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "atan2");
      }
    },

    ETY_FUNCTION_ATANH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.atanh (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "atanh");
      }
    },

    ETY_FUNCTION_BIN_DOLLAR {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        long x = elem.exlParamX.exlEval (mode).exlFloatValue.getl ();
        int m = Math.max (0, 63 - Long.numberOfLeadingZeros (x));  //桁数-1=最上位の桁位置
        char[] w = new char[64];
        for (int k = m; 0 <= k; k--) {  //桁位置
          int t = (int) (x >>> k) & 1;
          w[m - k] = (char) (48 + t);
        }
        elem.exlStringValue = String.valueOf (w, 0, m + 1);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "bin$");
      }
    },

    ETY_FUNCTION_CBRT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.cbrt (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cbrt");
      }
    },

    ETY_FUNCTION_CEIL {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.ceil (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "ceil");
      }
    },

    ETY_FUNCTION_CHR_DOLLAR {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = String.valueOf ((char) elem.exlParamX.exlEval (mode).exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "chr$");
      }
    },

    ETY_FUNCTION_CMP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.cmp (elem.exlParamY.exlEval (mode).exlFloatValue));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp");
      }
    },

    ETY_FUNCTION_CMP0 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.cmp0 ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp0");
      }
    },

    ETY_FUNCTION_CMP1 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.cmp1 ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp1");
      }
    },

    ETY_FUNCTION_CMP1ABS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.cmp1abs ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmp1abs");
      }
    },

    ETY_FUNCTION_CMPABS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.cmpabs (elem.exlParamY.exlEval (mode).exlFloatValue));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cmpabs");
      }
    },

    ETY_FUNCTION_COS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.cos (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cos");
      }
    },

    ETY_FUNCTION_COSH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.cosh (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cosh");
      }
    },

    ETY_FUNCTION_COT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.cot (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cot");
      }
    },

    ETY_FUNCTION_COTH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.coth (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "coth");
      }
    },

    ETY_FUNCTION_CSC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.csc (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "csc");
      }
    },

    ETY_FUNCTION_CSCH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.csch (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "csch");
      }
    },

    ETY_FUNCTION_CUB {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.cub (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "cub");
      }
    },

    ETY_FUNCTION_DEC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.dec (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "dec");
      }
    },

    ETY_FUNCTION_DEG {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.deg (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "deg");
      }
    },

    ETY_FUNCTION_DIV2 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.div2 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "div2");
      }
    },

    ETY_FUNCTION_DIV3 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.div3 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "div3");
      }
    },

    ETY_FUNCTION_DIVPI {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.divpi (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "divpi");
      }
    },

    ETY_FUNCTION_DIVRZ {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.divrz (elem.exlParamX.exlEval (mode).exlFloatValue,
                                  elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "divrz");
      }
    },

    ETY_FUNCTION_EXP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.exp (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp");
      }
    },

    ETY_FUNCTION_EXP10 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.exp10 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp10");
      }
    },

    ETY_FUNCTION_EXP2 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.exp2 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp2");
      }
    },

    ETY_FUNCTION_EXP2M1 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.exp2m1 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "exp2m1");
      }
    },

    ETY_FUNCTION_EXPM1 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.expm1 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "expm1");
      }
    },

    ETY_FUNCTION_FLOOR {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.floor (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "floor");
      }
    },

    ETY_FUNCTION_FRAC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.frac (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "frac");
      }
    },

    ETY_FUNCTION_GETEXP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.getexp (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "getexp");
      }
    },

    ETY_FUNCTION_GETMAN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.getman (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "getman");
      }
    },

    ETY_FUNCTION_HEX_DOLLAR {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        long x = elem.exlParamX.exlEval (mode).exlFloatValue.getl ();
        int m = Math.max (0, 63 - Long.numberOfLeadingZeros (x) >> 2);  //桁数-1=最上位の桁位置
        char[] w = new char[16];
        for (int k = m; 0 <= k; k--) {  //桁位置
          int t = (int) (x >>> (k << 2)) & 15;
          w[m - k] = (char) ((9 - t >> 4 & 7) + 48 + t);  //大文字
          //w[m - k] = (char) ((9 - t >> 4 & 39) + 48 + t);  //小文字
        }
        elem.exlStringValue = String.valueOf (w, 0, m + 1);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "hex$");
      }
    },

    ETY_FUNCTION_IEEEREM {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.ieeerem (elem.exlParamX.exlEval (mode).exlFloatValue,
                                    elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "ieeerem");
      }
    },

    ETY_FUNCTION_INC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.inc (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "inc");
      }
    },

    ETY_FUNCTION_ISEVEN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.iseven () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "iseven");
      }
    },

    ETY_FUNCTION_ISINF {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.isinf () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isinf");
      }
    },

    ETY_FUNCTION_ISINT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.isint () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isint");
      }
    },

    ETY_FUNCTION_ISNAN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.isnan () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isnan");
      }
    },

    ETY_FUNCTION_ISODD {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.isodd () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isodd");
      }
    },

    ETY_FUNCTION_ISONE {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.isone () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "isone");
      }
    },

    ETY_FUNCTION_ISZERO {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "iszero");
      }
    },

    ETY_FUNCTION_LGAMMA {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.lgamma (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "lgamma");
      }
    },

    ETY_FUNCTION_LOG {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.log (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log");
      }
    },

    ETY_FUNCTION_LOG10 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.log10 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log10");
      }
    },

    ETY_FUNCTION_LOG1P {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.log1p (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log1p");
      }
    },

    ETY_FUNCTION_LOG2 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.log2 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "log2");
      }
    },

    ETY_FUNCTION_MAX {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.max (elem.exlParamX.exlEval (mode).exlFloatValue,
                                elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "max");
      }
    },

    ETY_FUNCTION_MIN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.min (elem.exlParamX.exlEval (mode).exlFloatValue,
                                elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "min");
      }
    },

    ETY_FUNCTION_MUL2 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.mul2 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "mul2");
      }
    },

    ETY_FUNCTION_MUL3 {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.mul3 (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "mul3");
      }
    },

    ETY_FUNCTION_MULPI {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.mulpi (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "mulpi");
      }
    },

    ETY_FUNCTION_OCT_DOLLAR {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        long x = elem.exlParamX.exlEval (mode).exlFloatValue.getl ();
        int m = Math.max (0, (63 - Long.numberOfLeadingZeros (x)) / 3);  //桁数-1=最上位の桁位置
        char[] w = new char[22];
        for (int k = m; 0 <= k; k--) {  //桁位置
          int t = (int) (x >>> k * 3) & 7;
          w[m - k] = (char) (48 + t);
        }
        elem.exlStringValue = String.valueOf (w, 0, m + 1);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "oct$");
      }
    },

    ETY_FUNCTION_POW {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.pow (elem.exlParamX.exlEval (mode).exlFloatValue,
                                elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "pow");
      }
    },

    ETY_FUNCTION_QUO {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.quo (elem.exlParamX.exlEval (mode).exlFloatValue,
                                elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "quo");
      }
    },

    ETY_FUNCTION_RAD {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.rad (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rad");
      }
    },

    ETY_FUNCTION_RANDOM {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.random ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "random");
      }
    },

    ETY_FUNCTION_RCP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.rcp (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rcp");
      }
    },

    ETY_FUNCTION_RINT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.rint (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rint");
      }
    },

    ETY_FUNCTION_RMODE {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlSetRoundingMode (elem.exlParamX.exlEval (mode).exlFloatValue.geti ());
        elem.exlFloatValue.setnan ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rmode");
      }
    },

    ETY_FUNCTION_ROUND {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.round (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "round");
      }
    },

    ETY_FUNCTION_RPREC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlSetRoundingPrec (elem.exlParamX.exlEval (mode).exlFloatValue.geti ());
        elem.exlFloatValue.setnan ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "rprec");
      }
    },

    ETY_FUNCTION_SEC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sec (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sec");
      }
    },

    ETY_FUNCTION_SECH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sech (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sech");
      }
    },

    ETY_FUNCTION_SGN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sgn (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sgn");
      }
    },

    ETY_FUNCTION_SIN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sin (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sin");
      }
    },

    ETY_FUNCTION_SINH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sinh (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sinh");
      }
    },

    ETY_FUNCTION_SQRT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sqrt (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "sqrt");
      }
    },

    ETY_FUNCTION_SQU {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.squ (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "squ");
      }
    },

    ETY_FUNCTION_STR_DOLLAR {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = elem.exlParamX.exlEval (mode).exlFloatValue.toString ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "str$");
      }
    },

    ETY_FUNCTION_TAN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.tan (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "tan");
      }
    },

    ETY_FUNCTION_TANH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.tanh (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "tanh");
      }
    },

    ETY_FUNCTION_TGAMMA {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.tgamma (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "tgamma");
      }
    },

    ETY_FUNCTION_TRUNC {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.trunc (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "trunc");
      }
    },

    ETY_FUNCTION_ULP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.ulp (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "ulp");
      }
    },

    ETY_FUNCTION_VAL {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.parse (elem.exlParamX.exlEval (mode).exlStringValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendFunctionTo (sb, "val");
      }
    },

    //角括弧
    //  メモリ参照
    ETY_SQUARE_BRACKET {  // [x]
      @Override protected int etyPriority () {
        return EPY_PRIORITY_FUNCTION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int a, f;
        if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]
          a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
          f = elem.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
        } else {  // [x]
          a = elem.exlParamX.exlEval (mode).exlFloatValue.geti ();
          f = -1;
        }
        elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f));
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlParamX.exlAppendTo (sb.append ('[')).append (']');
      }
    },

    //＠演算子
    ETY_OPERATOR_AT {  // x@y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_AT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        //xとyを評価してxを返す
        elem.exlFloatValue.sete (elem.exlParamX.exlEval (mode).exlFloatValue);
        elem.exlParamY.exlEval (mode);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "@");
      }
    },

    //後置演算子
    ETY_OPERATOR_POSTINCREMENT {  // x++
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:  // x++
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue);
          elem.exlParamX.exlParamX.exlFloatValue.inc ();
          break;
        case ETY_INTEGER_REGISTER:  // d0++
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x + 1);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0++
          {
            int n = elem.exlParamX.exlSubscript;
            EFP x = elem.exlGetFPn (n);
            elem.exlFloatValue.sete (x);
            x.inc ();
          }
          break;
        case ETY_PC:  // pc++
          {
            int x = elem.exlReadPC ();
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x + 1);
          }
          break;
        case ETY_CCR:  // ccr++
          {
            int x = elem.exlReadCCR ();
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x + 1);
          }
          break;
        case ETY_SR:  // sr++
          {
            int x = elem.exlReadSR ();
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x + 1);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar++
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadFloatControlRegister (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x + 1);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc++
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadControlRegister (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x + 1);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]++
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]++
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x]++
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x + 1, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.b++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x + 1);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].b++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].b++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].b++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x + 1, f);
            }
            break;
          default:  // ?.b++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // x.w++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.w++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x + 1);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].w++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].w++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].w++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x + 1, f);
            }
            break;
          default:  // ?.w++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // x.l++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.l++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x + 1);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].l++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].l++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].l++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x + 1, f);
            }
            break;
          default:  // ?.l++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // x.q++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].q++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].q++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].q++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f);
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x + 1L, f);
            }
            break;
          default:  // ?.q++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.s++
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n));
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x + 1.0F));
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].s++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].s++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].s++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f));
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x + 1.0F), f);
            }
            break;
          default:  // ?.s++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].d++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].d++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].d++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f));
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x + 1.0), f);
            }
            break;
          default:  // ?.d++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].x++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].x++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].x++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setx012 (b, 0));
              x.inc ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ?.x++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].t++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].t++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].t++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.sety012 (b, 0));
              x.inc ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ?.t++
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // x.p++
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].p++
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].p++
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].p++
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setp012 (b, 0));
              x.inc ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ?.p++
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?++
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, "++");
      }
    },

    ETY_OPERATOR_POSTDECREMENT {  // x--
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:  // x--
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue);
          elem.exlParamX.exlParamX.exlFloatValue.dec ();
          break;
        case ETY_INTEGER_REGISTER:  // d0--
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x - 1);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0--
          {
            int n = elem.exlParamX.exlSubscript;
            EFP x = elem.exlGetFPn (n);
            elem.exlFloatValue.sete (x);
            x.dec ();
          }
          break;
        case ETY_PC:  // pc--
          {
            int x = elem.exlReadPC ();
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x - 1);
          }
          break;
        case ETY_CCR:  // ccr--
          {
            int x = elem.exlReadCCR ();
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x - 1);
          }
          break;
        case ETY_SR:  // sr--
          {
            int x = elem.exlReadSR ();
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x - 1);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar--
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadFloatControlRegister (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x - 1);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc--
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadControlRegister (n);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x - 1);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]--
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]--
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x]--
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x - 1, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.b--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x - 1);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].b--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].b--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].b--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x - 1, f);
            }
            break;
          default:  // ?.b--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // x.w--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.w--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x - 1);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].w--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].w--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].w--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x - 1, f);
            }
            break;
          default:  // ?.w--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // x.l--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.l--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x - 1);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].l--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].l--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].l--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x - 1, f);
            }
            break;
          default:  // ?.l--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // x.q--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].q--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].q--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].q--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f);
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x - 1L, f);
            }
            break;
          default:  // ?.q--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.s--
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n));
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x - 1.0F));
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].s--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].s--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].s--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f));
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x - 1.0F), f);
            }
            break;
          default:  // ?.s--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].d--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].d--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].d--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f));
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x - 1.0), f);
            }
            break;
          default:  // ?.d--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].x--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].x--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].x--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setx012 (b, 0));
              x.dec ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ?.x--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].t--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].t--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].t--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.sety012 (b, 0));
              x.dec ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ?.t--
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // x.p--
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].p--
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].p--
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].p--
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              EFP x = XEiJ.fpuBox.new EFP ();
              elem.exlFloatValue.sete (x.setp012 (b, 0));
              x.dec ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ?.p--
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?--
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, "--");
      }
    },

    ETY_OPERATOR_SIZE_BYTE {  // x.b
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].b
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].b
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].b
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f));
          }
          break;
        default:  // ?.b
          elem.exlFloatValue.seti ((byte) elem.exlParamX.exlEval (mode).exlFloatValue.geti ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".b");
      }
    },

    ETY_OPERATOR_SIZE_WORD {  // x.w
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].w
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].w
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].w
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f));
          }
          break;
        default:  // ?.w
          elem.exlFloatValue.seti ((short) elem.exlParamX.exlEval (mode).exlFloatValue.geti ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".w");
      }
    },

    ETY_OPERATOR_SIZE_LONG {  // x.l
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].l
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].l
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].l
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f));
          }
          break;
        default:  // ?.l
          elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.geti ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".l");
      }
    },

    ETY_OPERATOR_SIZE_QUAD {  // x.q
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].q
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].q
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].q
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f));
          }
          break;
        default:  // ?.q
          elem.exlFloatValue.setl (elem.exlParamX.exlEval (mode).exlFloatValue.getl ());
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".q");
      }
    },

    ETY_OPERATOR_SIZE_SINGLE {  // x.s
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].s
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].s
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].s
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f));
          }
          break;
        default:  // ?.s
          elem.exlFloatValue.sete (elem.exlParamX.exlEval (mode).exlFloatValue).roundf ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".s");
      }
    },

    ETY_OPERATOR_SIZE_DOUBLE {  // x.d
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].d
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].d
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].d
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f));
          }
          break;
        default:  // ?.d
          elem.exlFloatValue.sete (elem.exlParamX.exlEval (mode).exlFloatValue).roundd ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".d");
      }
    },

    ETY_OPERATOR_SIZE_EXTENDED {  // x.x
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].x
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].x
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].x
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            byte[] b = new byte[12];
            MC68060.mmuPeekExtended (a, b, f);
            elem.exlFloatValue.setx012 (b, 0);
          }
          break;
        default:  // ?.x
          elem.exlFloatValue.sete (elem.exlParamX.exlEval (mode).exlFloatValue).roundx ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".x");
      }
    },

    ETY_OPERATOR_SIZE_TRIPLE {  // x.t
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].t
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].t
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].t
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            byte[] b = new byte[12];
            MC68060.mmuPeekExtended (a, b, f);
            elem.exlFloatValue.sety012 (b, 0);
          }
          break;
        default:  // ?.t
          elem.exlFloatValue.sete (elem.exlParamX.exlEval (mode).exlFloatValue).roundy ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".t");
      }
    },

    ETY_OPERATOR_SIZE_PACKED {  // x.p
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_SQUARE_BRACKET:  // [x].p
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].p
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].p
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            byte[] b = new byte[12];
            MC68060.mmuPeekExtended (a, b, f);
            elem.exlFloatValue.setp012 (b, 0);
          }
          break;
        default:  // ?.p
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPostfixOperatorTo (sb, ".p");
      }
    },

    //前置演算子
    ETY_OPERATOR_PREINCREMENT {  // ++x
      @Override protected int etyPriority () {
        return EPY_PRIORITY_PREFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:  // ++x
          elem.exlParamX.exlParamX.exlFloatValue.inc ();
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue);
          break;
        case ETY_INTEGER_REGISTER:  // ++d0
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n) + 1;
            elem.exlWriteRegLong (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // ++fp0
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).inc ());
          break;
        case ETY_PC:  // ++pc
          {
            int x = elem.exlReadPC () + 1;
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // ++ccr
          {
            int x = elem.exlReadCCR () + 1;
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // ++sr
          {
            int x = elem.exlReadSR () + 1;
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // ++fpiar
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadFloatControlRegister (n) + 1;
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // ++sfc
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadControlRegister (n) + 1;
            elem.exlWriteControlRegister (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // ++[x]
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y]
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // ++[x]
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f) + 1;
            MC68060.mmuPokeByte (a, x, f);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // ++x.b
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // ++d0.b
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n) + 1;
              elem.exlWriteRegByte (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // ++[x].b
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].b
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].b
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f) + 1;
              MC68060.mmuPokeByte (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:  // ++?.b
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // ++x.w
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // ++d0.w
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n) + 1;
              elem.exlWriteRegWord (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // ++[x].w
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].w
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].w
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f) + 1;
              MC68060.mmuPokeWord (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:  // ++?.w
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // ++x.l
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // ++d0.l
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n) + 1;
              elem.exlWriteRegLong (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // ++[x].l
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].l
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].l
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f) + 1;
              MC68060.mmuPokeLong (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:  // ++?.l
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // ++x.q
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // ++[x].q
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].q
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].q
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f) + 1L;
              MC68060.mmuPokeQuad (a, x, f);
              elem.exlFloatValue.setl (x);
            }
            break;
          default:  // ++?.q
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // ++x.s
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // ++d0.s
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n)) + 1.0F;
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
              elem.exlFloatValue.setf (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // ++[x].s
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].s
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].s
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) + 1.0F;
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
              elem.exlFloatValue.setf (x);
            }
            break;
          default:  // ++?.s
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // ++x.d
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // ++[x].d
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].d
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].d
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) + 1.0;
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
              elem.exlFloatValue.setd (x);
            }
            break;
          default:  // ++?.d
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // ++x.x
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // ++[x].x
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].x
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].x
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setx012 (b, 0).inc ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ++?.x
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // ++x.t
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // ++[x].t
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].t
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].t
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.sety012 (b, 0).inc ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ++?.t
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // ++x.p
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // ++[x].p
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // ++[x@y].p
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // ++[x].p
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setp012 (b, 0).inc ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // ++?.p
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ++?
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "++");
      }
    },

    ETY_OPERATOR_PREDECREMENT {  // --x
      @Override protected int etyPriority () {
        return EPY_PRIORITY_PREFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:  // --x
          elem.exlParamX.exlParamX.exlFloatValue.dec ();
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue);
          break;
        case ETY_INTEGER_REGISTER:  // --d0
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadRegLong (n) - 1;
            elem.exlWriteRegLong (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // --fp0
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).dec ());
          break;
        case ETY_PC:  // --pc
          {
            int x = elem.exlReadPC () - 1;
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // --ccr
          {
            int x = elem.exlReadCCR () - 1;
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // --sr
          {
            int x = elem.exlReadSR () - 1;
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // --fpiar
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadFloatControlRegister (n) - 1;
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // --sfc
          {
            int n = elem.exlParamX.exlSubscript;
            int x = elem.exlReadControlRegister (n) - 1;
            elem.exlWriteControlRegister (n, x);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // --[x]
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y]
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // --[x]
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            int x = MC68060.mmuPeekByteSign (a, f) - 1;
            MC68060.mmuPokeByte (a, x, f);
            elem.exlFloatValue.seti (x);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // --x.b
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // --d0.b
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegByte (n) - 1;
              elem.exlWriteRegByte (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // --[x].b
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].b
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].b
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekByteSign (a, f) - 1;
              MC68060.mmuPokeByte (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:  // --?.b
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // --x.w
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // --d0.w
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegWord (n) - 1;
              elem.exlWriteRegWord (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // --[x].w
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].w
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].w
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekWordSign (a, f) - 1;
              MC68060.mmuPokeWord (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:  // --?.w
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // --x.l
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // --d0.l
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              int x = elem.exlReadRegLong (n) - 1;
              elem.exlWriteRegLong (n, x);
              elem.exlFloatValue.seti (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // --[x].l
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].l
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].l
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int x = MC68060.mmuPeekLong (a, f) - 1;
              MC68060.mmuPokeLong (a, x, f);
              elem.exlFloatValue.seti (x);
            }
            break;
          default:  // --?.l
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // --x.q
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // --[x].q
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].q
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].q
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              long x = MC68060.mmuPeekQuad (a, f) - 1L;
              MC68060.mmuPokeQuad (a, x, f);
              elem.exlFloatValue.setl (x);
            }
            break;
          default:  // --?.q
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // --x.s
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // --d0.s
            {
              int n = elem.exlParamX.exlParamX.exlSubscript;
              float x = Float.intBitsToFloat (elem.exlReadRegLong (n)) - 1.0F;
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
              elem.exlFloatValue.setf (x);
            }
            break;
          case ETY_SQUARE_BRACKET:  // --[x].s
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].s
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].s
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              float x = Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) - 1.0F;
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
              elem.exlFloatValue.setf (x);
            }
            break;
          default:  // --?.s
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // --x.d
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // --[x].d
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].d
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].d
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              double x = Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) - 1.0;
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
              elem.exlFloatValue.setd (x);
            }
            break;
          default:  // --?.d
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // --x.x
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // --[x].x
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].x
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].x
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setx012 (b, 0).dec ().getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // --?.x
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // --x.t
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // --[x].t
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].t
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].t
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.sety012 (b, 0).dec ().gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // --?.t
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // --x.p
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // --[x].p
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // --[x@y].p
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // --[x].p
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              elem.exlFloatValue.setp012 (b, 0).dec ().getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
            }
            break;
          default:  // --?.p
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // --?
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "--");
      }
    },

    ETY_OPERATOR_NOTHING {  // +x
      @Override protected int etyPriority () {
        return EPY_PRIORITY_PREFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sete (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_NEGATION {  // -x
      @Override protected int etyPriority () {
        return EPY_PRIORITY_PREFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.neg (elem.exlParamX.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "-");
      }
    },

    ETY_OPERATOR_BITWISE_NOT {  // ~x
      @Override protected int etyPriority () {
        return EPY_PRIORITY_PREFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setl (~elem.exlParamX.exlEval (mode).exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "~");
      }
    },

    ETY_OPERATOR_LOGICAL_NOT {  // !x
      @Override protected int etyPriority () {
        return EPY_PRIORITY_PREFIX;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "!");
      }
    },

    //累乗演算子
    ETY_OPERATOR_POWER {  // x**y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_EXPONENTIATION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.pow (elem.exlParamX.exlEval (mode).exlFloatValue, elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "**");
      }
    },

    //乗除算演算子
    ETY_OPERATOR_MULTIPLICATION {  // x*y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_MULTIPLICATION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.mul (elem.exlParamX.exlEval (mode).exlFloatValue, elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "*");
      }
    },

    ETY_OPERATOR_DIVISION {  // x/y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_MULTIPLICATION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.div (elem.exlParamX.exlEval (mode).exlFloatValue, elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "/");
      }
    },

    ETY_OPERATOR_MODULUS {  // x%y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_MULTIPLICATION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.rem (elem.exlParamX.exlEval (mode).exlFloatValue, elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "%");
      }
    },

    //加減算演算子
    ETY_OPERATOR_ADDITION_FLOAT_FLOAT {  // x+y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ADDITION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.add (elem.exlParamX.exlEval (mode).exlFloatValue, elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_ADDITION_FLOAT_STRING {  // x+y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ADDITION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = elem.exlParamX.exlEval (mode).exlFloatValue.toString () + elem.exlParamY.exlEval (mode).exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_ADDITION_STRING_FLOAT {  // x+y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ADDITION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = elem.exlParamX.exlEval (mode).exlStringValue + elem.exlParamY.exlEval (mode).exlFloatValue.toString ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_ADDITION_STRING_STRING {  // x+y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ADDITION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = elem.exlParamX.exlEval (mode).exlStringValue + elem.exlParamY.exlEval (mode).exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "+");
      }
    },

    ETY_OPERATOR_SUBTRACTION {  // x-y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ADDITION;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sub (elem.exlParamX.exlEval (mode).exlFloatValue, elem.exlParamY.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "-");
      }
    },

    //シフト演算子
    ETY_OPERATOR_LEFT_SHIFT {  // x<<y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_SHIFT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval (mode).exlFloatValue.getl () << elem.exlParamY.exlEval (mode).exlFloatValue.geti ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "<<");
      }
    },

    ETY_OPERATOR_RIGHT_SHIFT {  // x>>y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_SHIFT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval (mode).exlFloatValue.getl () >> elem.exlParamY.exlEval (mode).exlFloatValue.geti ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">>");
      }
    },

    ETY_OPERATOR_UNSIGNED_RIGHT_SHIFT {  // x>>>y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_SHIFT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval (mode).exlFloatValue.getl () >>> elem.exlParamY.exlEval (mode).exlFloatValue.geti ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">>>");
      }
    },

    //比較演算子
    ETY_OPERATOR_LESS_THAN {  // x<y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMPARISON;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.lt (elem.exlParamY.exlEval (mode).exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "<");
      }
    },

    ETY_OPERATOR_LESS_OR_EQUAL {  // x<=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMPARISON;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.le (elem.exlParamY.exlEval (mode).exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "<=");
      }
    },

    ETY_OPERATOR_GREATER_THAN {  // x>y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMPARISON;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.gt (elem.exlParamY.exlEval (mode).exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">");
      }
    },

    ETY_OPERATOR_GREATER_OR_EQUAL {  // x>=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMPARISON;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.ge (elem.exlParamY.exlEval (mode).exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ">=");
      }
    },

    //等価演算子
    ETY_OPERATOR_EQUAL {  // x==y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_EQUALITY;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.eq (elem.exlParamY.exlEval (mode).exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "==");
      }
    },

    ETY_OPERATOR_NOT_EQUAL {  // x!=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_EQUALITY;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (elem.exlParamX.exlEval (mode).exlFloatValue.ne (elem.exlParamY.exlEval (mode).exlFloatValue) ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "!=");
      }
    },

    //ビットAND演算子
    ETY_OPERATOR_BITWISE_AND {  // x&y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_BITWISE_AND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval (mode).exlFloatValue.getl () &
                                 elem.exlParamY.exlEval (mode).exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "&");
      }
    },

    //ビットXOR演算子
    ETY_OPERATOR_BITWISE_XOR {  // x^y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_BITWISE_XOR;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval (mode).exlFloatValue.getl () ^
                                 elem.exlParamY.exlEval (mode).exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "^");
      }
    },

    //ビットOR演算子
    ETY_OPERATOR_BITWISE_OR {  // x|y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_BITWISE_OR;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.setl (elem.exlParamX.exlEval (mode).exlFloatValue.getl () |
                                 elem.exlParamY.exlEval (mode).exlFloatValue.getl ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "|");
      }
    },

    //論理AND演算子
    ETY_OPERATOR_LOGICAL_AND {  // x&&y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_LOGICAL_AND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (!elem.exlParamX.exlEval (mode).exlFloatValue.iszero () &&
                                 !elem.exlParamY.exlEval (mode).exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "&&");
      }
    },

    //論理OR演算子
    ETY_OPERATOR_LOGICAL_OR {  // x||y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_LOGICAL_OR;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.seti (!elem.exlParamX.exlEval (mode).exlFloatValue.iszero () ||
                                 !elem.exlParamY.exlEval (mode).exlFloatValue.iszero () ? 1 : 0);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "||");
      }
    },

    //条件演算子
    ETY_OPERATOR_CONDITIONAL_FLOAT {  // x?y:z
      @Override protected int etyPriority () {
        return EPY_PRIORITY_CONDITIONAL;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlFloatValue.sete (!elem.exlParamX.exlEval (mode).exlFloatValue.iszero () ?
                                 elem.exlParamY.exlEval (mode).exlFloatValue :
                                 elem.exlParamZ.exlEval (mode).exlFloatValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendConditionalOperatorTo (sb, "?", ":");
      }
    },

    ETY_OPERATOR_CONDITIONAL_STRING {  // x?y:z
      @Override protected int etyPriority () {
        return EPY_PRIORITY_CONDITIONAL;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = (!elem.exlParamX.exlEval (mode).exlFloatValue.iszero () ?
                               elem.exlParamY.exlEval (mode).exlStringValue :
                               elem.exlParamZ.exlEval (mode).exlStringValue);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendConditionalOperatorTo (sb, "?", ":");
      }
    },

    //代入演算子
    ETY_OPERATOR_ASSIGNMENT {  // x=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:  // x=y
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.sete (elem.exlParamY.exlEval (mode).exlFloatValue));
          break;
        case ETY_INTEGER_REGISTER:  // d0=y
          {
            int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWriteRegLong (elem.exlParamX.exlSubscript, y);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0=y
          {
            EFP y = elem.exlParamY.exlEval (mode).exlFloatValue;
            elem.exlFloatValue.sete (y);
            elem.exlGetFPn (elem.exlParamX.exlSubscript).sete (y);
          }
          break;
        case ETY_PC:  // pc=y
          {
            int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWritePC (y);
          }
          break;
        case ETY_CCR:  // ccr=y
          {
            int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWriteCCR (y);
          }
          break;
        case ETY_SR:  // sr=y
          {
            int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWriteSR (y);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar=y
          {
            int n = elem.exlParamX.exlSubscript;
            int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWriteFloatControlRegister (n, y);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc=y
          {
            int n = elem.exlParamX.exlSubscript;
            int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
            elem.exlFloatValue.seti (y);
            elem.exlWriteControlRegister (n, y);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]=y
          {
            int a, f;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x]=y
              a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamY.exlValueType == ElementType.ETY_FLOAT) {  // 浮動小数点数
              int y = (byte) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              elem.exlFloatValue.seti (y);
              MC68060.mmuPokeByte (a, y, f);
            } else {  // 文字列
              MC68060.mmuPokeStringZ (a, elem.exlStringValue = elem.exlParamY.exlEval (mode).exlStringValue, f);
            }
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.b=y
            {
              int y = (byte) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              elem.exlWriteRegByte (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.seti (y);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].b=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].b=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].b=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int y = (byte) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              MC68060.mmuPokeByte (a, y, f);
              elem.exlFloatValue.seti (y);
            }
            break;
          default:  // ?.b=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_WORD:  // x.w=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.w=y
            {
              int y = (short) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              elem.exlWriteRegWord (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.seti (y);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].w=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].w=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].w=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int y = (short) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              MC68060.mmuPokeWord (a, y, f);
              elem.exlFloatValue.seti (y);
            }
            break;
          default:  // ?.w=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_LONG:  // x.l=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.l=y
            {
              int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              elem.exlWriteRegLong (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.seti (y);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].l=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].l=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].l=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int y = (int) elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              MC68060.mmuPokeLong (a, y, f);
              elem.exlFloatValue.seti (y);
            }
            break;
          default:  // ?.l=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_QUAD:  // x.q=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].q=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].q=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].q=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              long y = elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
              MC68060.mmuPokeQuad (a, y, f);
              elem.exlFloatValue.setl (y);
            }
            break;
          default:  // ?.q=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_INTEGER_REGISTER:  // d0.s=y
            {
              int y = elem.exlParamY.exlEval (mode).exlFloatValue.getf0 ();
              elem.exlWriteRegLong (elem.exlParamX.exlParamX.exlSubscript, y);
              elem.exlFloatValue.setf0 (y);
            }
            break;
          case ETY_SQUARE_BRACKET:  // [x].s=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].s=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].s=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              int y = elem.exlParamY.exlEval (mode).exlFloatValue.getf0 ();
              MC68060.mmuPokeLong (a, y, f);
              elem.exlFloatValue.setf0 (y);
            }
            break;
          default:  // ?.s=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].d=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].d=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].d=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              long y = elem.exlParamY.exlEval (mode).exlFloatValue.getd01 ();
              MC68060.mmuPokeQuad (a, y, f);
              elem.exlFloatValue.setd01 (y);
            }
            break;
          default:  // ?.d=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].x=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].x=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].x=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              elem.exlParamY.exlEval (mode).exlFloatValue.getx012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
              elem.exlFloatValue.setx012 (b, 0);
            }
            break;
          default:  // ?.x=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].t=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].t=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].t=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              elem.exlParamY.exlEval (mode).exlFloatValue.gety012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
              elem.exlFloatValue.sety012 (b, 0);
            }
            break;
          default:  // ?.t=y
            elem.exlFloatValue.setnan ();
          }
          break;
        case ETY_OPERATOR_SIZE_PACKED:  // x.p=y
          switch (elem.exlParamX.exlParamX.exlType) {
          case ETY_SQUARE_BRACKET:  // [x].p=y
            {
              int a, f;
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].p=y
                a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
              } else {  // [x].p=y
                a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
                f = -1;
              }
              byte[] b = new byte[12];
              elem.exlParamY.exlEval (mode).exlFloatValue.getp012 (b, 0);
              MC68060.mmuPokeExtended (a, b, f);
              elem.exlFloatValue.setp012 (b, 0);
            }
            break;
          default:  // ?.p=y
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "=");
      }
    },

    ETY_OPERATOR_SELF_POWER {  // x**=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval (mode).exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.pow (y));
          break;
        case ETY_INTEGER_REGISTER:  // d0**=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).pow (y).geti ());
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0**=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).pow (y));
          break;
        case ETY_PC:  // pc**=y
          elem.exlWritePC (elem.exlFloatValue.seti (elem.exlReadPC ()).pow (y).geti ());
          break;
        case ETY_CCR:  // ccr**=y
          elem.exlWriteCCR (elem.exlFloatValue.seti (elem.exlReadCCR ()).pow (y).geti ());
          break;
        case ETY_SR:  // sr**=y
          elem.exlWriteSR (elem.exlFloatValue.seti (elem.exlReadSR ()).pow (y).geti ());
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar**=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteFloatControlRegister (n, elem.exlFloatValue.seti (elem.exlReadFloatControlRegister (n)).pow (y).geti ());
          break;
        case ETY_CONTROL_REGISTER:  // sfc**=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteControlRegister (n, elem.exlFloatValue.seti (elem.exlReadControlRegister (n)).pow (y).geti ());
          break;
        case ETY_SQUARE_BRACKET:  // [x]**=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]**=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]**=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).pow (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b**=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w**=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l**=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q**=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s**=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d**=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x**=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t**=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p**=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?**=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b**=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).pow (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w**=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).pow (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l**=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).pow (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s**=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).pow (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?**=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?**=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?**=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b**=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).pow (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w**=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).pow (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l**=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).pow (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q**=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).pow (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s**=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).pow (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d**=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).pow (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x**=y
                elem.exlFloatValue.setx012 (b, 0).pow (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t**=y
                elem.exlFloatValue.sety012 (b, 0).pow (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p**=y
                elem.exlFloatValue.setp012 (b, 0).pow (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?**=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "**=");
      }
    },

    ETY_OPERATOR_SELF_MULTIPLICATION {  // x*=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval (mode).exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.mul (y));
          break;
        case ETY_INTEGER_REGISTER:  // d0*=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).mul (y).geti ());
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0*=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).mul (y));
          break;
        case ETY_PC:  // pc*=y
          elem.exlWritePC (elem.exlFloatValue.seti (elem.exlReadPC ()).mul (y).geti ());
          break;
        case ETY_CCR:  // ccr*=y
          elem.exlWriteCCR (elem.exlFloatValue.seti (elem.exlReadCCR ()).mul (y).geti ());
          break;
        case ETY_SR:  // sr*=y
          elem.exlWriteSR (elem.exlFloatValue.seti (elem.exlReadSR ()).mul (y).geti ());
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar*=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteFloatControlRegister (n, elem.exlFloatValue.seti (elem.exlReadFloatControlRegister (n)).mul (y).geti ());
          break;
        case ETY_CONTROL_REGISTER:  // sfc*=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteControlRegister (n, elem.exlFloatValue.seti (elem.exlReadControlRegister (n)).mul (y).geti ());
          break;
        case ETY_SQUARE_BRACKET:  // [x]*=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]*=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]*=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).mul (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b*=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w*=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l*=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q*=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s*=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d*=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x*=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t*=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p*=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?*=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b*=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).mul (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w*=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).mul (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l*=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).mul (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s*=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).mul (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?*=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?*=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?*=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b*=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).mul (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w*=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).mul (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l*=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).mul (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q*=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).mul (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s*=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).mul (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d*=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).mul (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x*=y
                elem.exlFloatValue.setx012 (b, 0).mul (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t*=y
                elem.exlFloatValue.sety012 (b, 0).mul (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p*=y
                elem.exlFloatValue.setp012 (b, 0).mul (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?*=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "*=");
      }
    },

    ETY_OPERATOR_SELF_DIVISION {  // x/=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval (mode).exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.div (y));
          break;
        case ETY_INTEGER_REGISTER:  // d0/=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).div (y).geti ());
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0/=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).div (y));
          break;
        case ETY_PC:  // pc/=y
          elem.exlWritePC (elem.exlFloatValue.seti (elem.exlReadPC ()).div (y).geti ());
          break;
        case ETY_CCR:  // ccr/=y
          elem.exlWriteCCR (elem.exlFloatValue.seti (elem.exlReadCCR ()).div (y).geti ());
          break;
        case ETY_SR:  // sr/=y
          elem.exlWriteSR (elem.exlFloatValue.seti (elem.exlReadSR ()).div (y).geti ());
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar/=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteFloatControlRegister (n, elem.exlFloatValue.seti (elem.exlReadFloatControlRegister (n)).div (y).geti ());
          break;
        case ETY_CONTROL_REGISTER:  // sfc/=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteControlRegister (n, elem.exlFloatValue.seti (elem.exlReadControlRegister (n)).div (y).geti ());
          break;
        case ETY_SQUARE_BRACKET:  // [x]/=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]/=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]/=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).div (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b/=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w/=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l/=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q/=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s/=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d/=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x/=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t/=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p/=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?/=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b/=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).div (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w/=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).div (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l/=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).div (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s/=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).div (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?/=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?/=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?/=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b/=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).div (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w/=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).div (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l/=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).div (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q/=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).div (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s/=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).div (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d/=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).div (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x/=y
                elem.exlFloatValue.setx012 (b, 0).div (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t/=y
                elem.exlFloatValue.sety012 (b, 0).div (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p/=y
                elem.exlFloatValue.setp012 (b, 0).div (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?/=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "/=");
      }
    },

    ETY_OPERATOR_SELF_MODULUS {  // x%=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval (mode).exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.rem (y));
          break;
        case ETY_INTEGER_REGISTER:  // d0%=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).rem (y).geti ());
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0%=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).rem (y));
          break;
        case ETY_PC:  // pc%=y
          elem.exlWritePC (elem.exlFloatValue.seti (elem.exlReadPC ()).rem (y).geti ());
          break;
        case ETY_CCR:  // ccr%=y
          elem.exlWriteCCR (elem.exlFloatValue.seti (elem.exlReadCCR ()).rem (y).geti ());
          break;
        case ETY_SR:  // sr%=y
          elem.exlWriteSR (elem.exlFloatValue.seti (elem.exlReadSR ()).rem (y).geti ());
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar%=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteFloatControlRegister (n, elem.exlFloatValue.seti (elem.exlReadFloatControlRegister (n)).rem (y).geti ());
          break;
        case ETY_CONTROL_REGISTER:  // sfc%=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteControlRegister (n, elem.exlFloatValue.seti (elem.exlReadControlRegister (n)).rem (y).geti ());
          break;
        case ETY_SQUARE_BRACKET:  // [x]%=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]%=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]%=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).rem (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b%=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w%=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l%=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q%=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s%=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d%=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x%=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t%=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p%=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?%=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b%=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).rem (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w%=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).rem (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l%=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).rem (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s%=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).rem (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?%=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?%=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?%=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b%=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).rem (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w%=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).rem (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l%=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).rem (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q%=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).rem (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s%=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).rem (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d%=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).rem (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x%=y
                elem.exlFloatValue.setx012 (b, 0).rem (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t%=y
                elem.exlFloatValue.sety012 (b, 0).rem (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p%=y
                elem.exlFloatValue.setp012 (b, 0).rem (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?%=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "%=");
      }
    },

    ETY_OPERATOR_SELF_ADDITION {  // x+=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval (mode).exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.add (y));
          break;
        case ETY_INTEGER_REGISTER:  // d0+=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).add (y).geti ());
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0+=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).add (y));
          break;
        case ETY_PC:  // pc+=y
          elem.exlWritePC (elem.exlFloatValue.seti (elem.exlReadPC ()).add (y).geti ());
          break;
        case ETY_CCR:  // ccr+=y
          elem.exlWriteCCR (elem.exlFloatValue.seti (elem.exlReadCCR ()).add (y).geti ());
          break;
        case ETY_SR:  // sr+=y
          elem.exlWriteSR (elem.exlFloatValue.seti (elem.exlReadSR ()).add (y).geti ());
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar+=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteFloatControlRegister (n, elem.exlFloatValue.seti (elem.exlReadFloatControlRegister (n)).add (y).geti ());
          break;
        case ETY_CONTROL_REGISTER:  // sfc+=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteControlRegister (n, elem.exlFloatValue.seti (elem.exlReadControlRegister (n)).add (y).geti ());
          break;
        case ETY_SQUARE_BRACKET:  // [x]+=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]+=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]+=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).add (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b+=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w+=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l+=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q+=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s+=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d+=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x+=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t+=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p+=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?+=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b+=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).add (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w+=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).add (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l+=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).add (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s+=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).add (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?+=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?+=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?+=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b+=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).add (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w+=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).add (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l+=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).add (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q+=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).add (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s+=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).add (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d+=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).add (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x+=y
                elem.exlFloatValue.setx012 (b, 0).add (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t+=y
                elem.exlFloatValue.sety012 (b, 0).add (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p+=y
                elem.exlFloatValue.setp012 (b, 0).add (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?+=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "+=");
      }
    },

    ETY_OPERATOR_SELF_SUBTRACTION {  // x-=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        EFPBox.EFP y = elem.exlParamY.exlEval (mode).exlFloatValue;
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.sub (y));
          break;
        case ETY_INTEGER_REGISTER:  // d0-=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).sub (y).geti ());
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0-=y
          elem.exlFloatValue.sete (elem.exlGetFPn (elem.exlParamX.exlSubscript).sub (y));
          break;
        case ETY_PC:  // pc-=y
          elem.exlWritePC (elem.exlFloatValue.seti (elem.exlReadPC ()).sub (y).geti ());
          break;
        case ETY_CCR:  // ccr-=y
          elem.exlWriteCCR (elem.exlFloatValue.seti (elem.exlReadCCR ()).sub (y).geti ());
          break;
        case ETY_SR:  // sr-=y
          elem.exlWriteSR (elem.exlFloatValue.seti (elem.exlReadSR ()).sub (y).geti ());
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar-=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteFloatControlRegister (n, elem.exlFloatValue.seti (elem.exlReadFloatControlRegister (n)).sub (y).geti ());
          break;
        case ETY_CONTROL_REGISTER:  // sfc-=y
          n = elem.exlParamX.exlSubscript;
          elem.exlWriteControlRegister (n, elem.exlFloatValue.seti (elem.exlReadControlRegister (n)).sub (y).geti ());
          break;
        case ETY_SQUARE_BRACKET:  // [x]-=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]-=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]-=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).sub (y).geti (), f);
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b-=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w-=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l-=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q-=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s-=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d-=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x-=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t-=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p-=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?-=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b-=y
              elem.exlWriteRegByte (n, elem.exlFloatValue.seti (elem.exlReadRegByte (n)).sub (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w-=y
              elem.exlWriteRegWord (n, elem.exlFloatValue.seti (elem.exlReadRegWord (n)).sub (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l-=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.seti (elem.exlReadRegLong (n)).sub (y).geti ());
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s-=y
              elem.exlWriteRegLong (n, elem.exlFloatValue.setf0 (elem.exlReadRegLong (n)).sub (y).getf0 ());
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?-=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?-=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x]./-=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b-=y
              MC68060.mmuPokeByte (a, elem.exlFloatValue.seti (MC68060.mmuPeekByteSign (a, f)).sub (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w-=y
              MC68060.mmuPokeWord (a, elem.exlFloatValue.seti (MC68060.mmuPeekWordSign (a, f)).sub (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l-=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.seti (MC68060.mmuPeekLong (a, f)).sub (y).geti (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q-=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setl (MC68060.mmuPeekQuad (a, f)).sub (y).getl (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s-=y
              MC68060.mmuPokeLong (a, elem.exlFloatValue.setf0 (MC68060.mmuPeekLong (a, f)).sub (y).getf0 (), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d-=y
              MC68060.mmuPokeQuad (a, elem.exlFloatValue.setd01 (MC68060.mmuPeekQuad (a, f)).sub (y).getd01 (), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x-=y
                elem.exlFloatValue.setx012 (b, 0).sub (y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t-=y
                elem.exlFloatValue.sety012 (b, 0).sub (y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p-=y
                elem.exlFloatValue.setp012 (b, 0).sub (y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?-=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "-=");
      }
    },

    ETY_OPERATOR_SELF_LEFT_SHIFT {  // x<<=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        int y = elem.exlParamY.exlEval (mode).exlFloatValue.geti ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () << y));
          break;
        case ETY_INTEGER_REGISTER:  // d0<<=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) << y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0<<=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () << y));
          break;
        case ETY_PC:  // pc<<=y
          {
            int x = (int) ((long) elem.exlReadPC () << y);
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // ccr<<=y
          {
            int x = (int) ((long) elem.exlReadCCR () << y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // sr<<=y
          {
            int x = (int) ((long) elem.exlReadSR () << y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar<<=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadFloatControlRegister (n) << y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc<<=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadControlRegister (n) << y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]<<=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]<<=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]<<=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) << y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b<<=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w<<=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l<<=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q<<=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s<<=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d<<=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x<<=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t<<=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p<<=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?<<=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b<<=y
              int x = (byte) ((long) elem.exlReadRegByte (n) << y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w<<=y
              int x = (short) ((long) elem.exlReadRegWord (n) << y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l<<=y
              int x = (int) ((long) elem.exlReadRegLong (n) << y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s<<=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) << y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?<<=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?<<=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?<<=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b<<=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) << y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w<<=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) << y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l<<=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) << y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q<<=y
              long x = MC68060.mmuPeekQuad (a, f) << y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s<<=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) << y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d<<=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) << y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x<<=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () << y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t<<=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () << y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p<<=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () << y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?<<=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "<<=");
      }
    },

    ETY_OPERATOR_SELF_RIGHT_SHIFT {  // x>>=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        int y = elem.exlParamY.exlEval (mode).exlFloatValue.geti ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () >> y));
          break;
        case ETY_INTEGER_REGISTER:  // d0>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0>>=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () >> y));
          break;
        case ETY_PC:  // pc>>=y
          {
            int x = (int) ((long) elem.exlReadPC () >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // ccr>>=y
          {
            int x = (int) ((long) elem.exlReadCCR () >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // sr>>=y
          {
            int x = (int) ((long) elem.exlReadSR () >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadFloatControlRegister (n) >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadControlRegister (n) >> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]>>=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]>>=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]>>=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >> y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b>>=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w>>=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l>>=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q>>=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s>>=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d>>=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x>>=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t>>=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p>>=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?>>=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b>>=y
              int x = (byte) ((long) elem.exlReadRegByte (n) >> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w>>=y
              int x = (short) ((long) elem.exlReadRegWord (n) >> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l>>=y
              int x = (int) ((long) elem.exlReadRegLong (n) >> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s>>=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) >> y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?>>=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b>>=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w>>=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) >> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l>>=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) >> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q>>=y
              long x = MC68060.mmuPeekQuad (a, f) >> y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s>>=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) >> y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d>>=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) >> y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () >> y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () >> y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () >> y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?>>=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, ">>=");
      }
    },

    ETY_OPERATOR_SELF_UNSIGNED_RIGHT_SHIFT {  // x>>>=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        int y = elem.exlParamY.exlEval (mode).exlFloatValue.geti ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () >>> y));
          break;
        case ETY_INTEGER_REGISTER:  // d0>>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0>>>=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () >>> y));
          break;
        case ETY_PC:  // pc>>>=y
          {
            int x = (int) ((long) elem.exlReadPC () >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // ccr>>>=y
          {
            int x = (int) ((long) elem.exlReadCCR () >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // sr>>>=y
          {
            int x = (int) ((long) elem.exlReadSR () >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar>>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadFloatControlRegister (n) >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc>>>=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadControlRegister (n) >>> y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]>>>=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]>>>=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]>>>=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >>> y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b>>>=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w>>>=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l>>>=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q>>>=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s>>>=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d>>>=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x>>>=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t>>>=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p>>>=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?>>>=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b>>>=y
              int x = (byte) ((long) elem.exlReadRegByte (n) >>> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w>>>=y
              int x = (short) ((long) elem.exlReadRegWord (n) >>> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l>>>=y
              int x = (int) ((long) elem.exlReadRegLong (n) >>> y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s>>>=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) >>> y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?>>>=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?>>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?>>>=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b>>>=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) >>> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w>>>=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) >>> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l>>>=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) >>> y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q>>>=y
              long x = MC68060.mmuPeekQuad (a, f) >>> y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s>>>=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) >>> y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d>>>=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) >>> y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x>>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () >>> y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t>>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () >>> y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p>>>=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () >>> y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?>>>=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, ">>>=");
      }
    },

    ETY_OPERATOR_SELF_BITWISE_AND {  // x&=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        long y = elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () & y));
          break;
        case ETY_INTEGER_REGISTER:  // d0&=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) & y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0&=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () & y));
          break;
        case ETY_PC:  // pc&=y
          {
            int x = (int) ((long) elem.exlReadPC () & y);
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // ccr&=y
          {
            int x = (int) ((long) elem.exlReadCCR () & y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // sr&=y
          {
            int x = (int) ((long) elem.exlReadSR () & y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar&=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadFloatControlRegister (n) & y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc&=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadControlRegister (n) & y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]&=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]&=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]&=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) & y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b&=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w&=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l&=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q&=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s&=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d&=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x&=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t&=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p&=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?&=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b&=y
              int x = (byte) ((long) elem.exlReadRegByte (n) & y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w&=y
              int x = (short) ((long) elem.exlReadRegWord (n) & y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l&=y
              int x = (int) ((long) elem.exlReadRegLong (n) & y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s&=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) & y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?&=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?&=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?&=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b&=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) & y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w&=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) & y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l&=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) & y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q&=y
              long x = MC68060.mmuPeekQuad (a, f) & y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s&=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) & y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d&=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) & y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x&=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () & y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t&=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () & y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p&=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () & y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?&=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "&=");
      }
    },

    ETY_OPERATOR_SELF_BITWISE_XOR {  // x^=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        long y = elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () ^ y));
          break;
        case ETY_INTEGER_REGISTER:  // d0^=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0^=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () ^ y));
          break;
        case ETY_PC:  // pc^=y
          {
            int x = (int) ((long) elem.exlReadPC () ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // ccr^=y
          {
            int x = (int) ((long) elem.exlReadCCR () ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // sr^=y
          {
            int x = (int) ((long) elem.exlReadSR () ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar^=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadFloatControlRegister (n) ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc^=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadControlRegister (n) ^ y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]^=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]^=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]^=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) ^ y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b^=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w^=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l^=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q^=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s^=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d^=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x^=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t^=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p^=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?^=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b^=y
              int x = (byte) ((long) elem.exlReadRegByte (n) ^ y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w^=y
              int x = (short) ((long) elem.exlReadRegWord (n) ^ y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l^=y
              int x = (int) ((long) elem.exlReadRegLong (n) ^ y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s^=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) ^ y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?^=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?^=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?^=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b^=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) ^ y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w^=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) ^ y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l^=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) ^ y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q^=y
              long x = MC68060.mmuPeekQuad (a, f) ^ y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s^=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) ^ y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d^=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) ^ y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x^=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () ^ y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t^=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () ^ y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p^=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () ^ y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?^=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "^=");
      }
    },

    ETY_OPERATOR_SELF_BITWISE_OR {  // x|=y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int n, a, f;
        long y = elem.exlParamY.exlEval (mode).exlFloatValue.getl ();
        switch (elem.exlParamX.exlType) {
        case ETY_VARIABLE_FLOAT:
          elem.exlFloatValue.sete (elem.exlParamX.exlParamX.exlFloatValue.setl (elem.exlParamX.exlParamX.exlFloatValue.getl () | y));
          break;
        case ETY_INTEGER_REGISTER:  // d0|=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadRegLong (n) | y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteRegLong (n, x);
          }
          break;
        case ETY_FLOATING_POINT_REGISTER:  // fp0|=y
          n = elem.exlParamX.exlSubscript;
          elem.exlSetFPn (n, elem.exlFloatValue.setl (elem.exlGetFPn (n).getl () | y));
          break;
        case ETY_PC:  // pc|=y
          {
            int x = (int) ((long) elem.exlReadPC () | y);
            elem.exlFloatValue.seti (x);
            elem.exlWritePC (x);
          }
          break;
        case ETY_CCR:  // ccr|=y
          {
            int x = (int) ((long) elem.exlReadCCR () | y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteCCR (x);
          }
          break;
        case ETY_SR:  // sr|=y
          {
            int x = (int) ((long) elem.exlReadSR () | y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteSR (x);
          }
          break;
        case ETY_FLOAT_CONTROL_REGISTER:  // fpiar|=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadFloatControlRegister (n) | y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteFloatControlRegister (n, x);
          }
          break;
        case ETY_CONTROL_REGISTER:  // sfc|=y
          n = elem.exlParamX.exlSubscript;
          {
            int x = (int) ((long) elem.exlReadControlRegister (n) | y);
            elem.exlFloatValue.seti (x);
            elem.exlWriteControlRegister (n, x);
          }
          break;
        case ETY_SQUARE_BRACKET:  // [x]|=y
          if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y]|=y
            a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = elem.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
          } else {  // [x]|=y
            a = elem.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
            f = -1;
          }
          {
            int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) | y);
            elem.exlFloatValue.seti (x);
            MC68060.mmuPokeByte (a, x, f);
          }
          break;
        case ETY_OPERATOR_SIZE_BYTE:  // x.b|=y
        case ETY_OPERATOR_SIZE_WORD:  // x.w|=y
        case ETY_OPERATOR_SIZE_LONG:  // x.l|=y
        case ETY_OPERATOR_SIZE_QUAD:  // x.q|=y
        case ETY_OPERATOR_SIZE_SINGLE:  // x.s|=y
        case ETY_OPERATOR_SIZE_DOUBLE:  // x.d|=y
        case ETY_OPERATOR_SIZE_EXTENDED:  // x.x|=y
        case ETY_OPERATOR_SIZE_TRIPLE:  // x.t|=y
        case ETY_OPERATOR_SIZE_PACKED:  // x.p|=y
          if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // d0.?|=y
            n = elem.exlParamX.exlParamX.exlSubscript;
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // d0.b|=y
              int x = (byte) ((long) elem.exlReadRegByte (n) | y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegByte (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // d0.w|=y
              int x = (short) ((long) elem.exlReadRegWord (n) | y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegWord (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // d0.l|=y
              int x = (int) ((long) elem.exlReadRegLong (n) | y);
              elem.exlFloatValue.seti (x);
              elem.exlWriteRegLong (n, x);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // d0.s|=y
              float x = (float) ((long) Float.intBitsToFloat (elem.exlReadRegLong (n)) | y);
              elem.exlFloatValue.setf (x);
              elem.exlWriteRegLong (n, Float.floatToIntBits (x));
            } else {
              elem.exlFloatValue.setnan ();
            }
          } else if (elem.exlParamX.exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET) {  // [x].?|=y
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_AT) {  // [x@y].?|=y
              a = elem.exlParamX.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = elem.exlParamX.exlParamX.exlParamX.exlParamY.exlEval (mode).exlFloatValue.geti ();
            } else {  // [x].?|=y
              a = elem.exlParamX.exlParamX.exlParamX.exlEval (mode).exlFloatValue.geti ();
              f = -1;
            }
            if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE) {  // [x].b|=y
              int x = (byte) ((long) MC68060.mmuPeekByteSign (a, f) | y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeByte (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_WORD) {  // [x].w|=y
              int x = (short) ((long) MC68060.mmuPeekWordSign (a, f) | y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeWord (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) {  // [x].l|=y
              int x = (int) ((long) MC68060.mmuPeekLong (a, f) | y);
              elem.exlFloatValue.seti (x);
              MC68060.mmuPokeLong (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD) {  // [x].q|=y
              long x = MC68060.mmuPeekQuad (a, f) | y;
              elem.exlFloatValue.setl (x);
              MC68060.mmuPokeQuad (a, x, f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) {  // [x].s|=y
              float x = (float) ((long) Float.intBitsToFloat (MC68060.mmuPeekLong (a, f)) | y);
              elem.exlFloatValue.setf (x);
              MC68060.mmuPokeLong (a, Float.floatToIntBits (x), f);
            } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE) {  // [x].d|=y
              double x = (double) ((long) Double.longBitsToDouble (MC68060.mmuPeekQuad (a, f)) | y);
              elem.exlFloatValue.setd (x);
              MC68060.mmuPokeQuad (a, Double.doubleToLongBits (x), f);
            } else {
              byte[] b = new byte[12];
              MC68060.mmuPeekExtended (a, b, f);
              if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED) {  // [x].x|=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setx012 (b, 0).getl () | y).getx012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE) {  // [x].t|=y
                elem.exlFloatValue.setl (elem.exlFloatValue.sety012 (b, 0).getl () | y).gety012 (b, 0);
              } else if (elem.exlParamX.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) {  // [x].p|=y
                elem.exlFloatValue.setl (elem.exlFloatValue.setp012 (b, 0).getl () | y).getp012 (b, 0);
              } else {
                elem.exlFloatValue.setnan ();
              }
              MC68060.mmuPokeExtended (a, b, f);
            }
          } else {
            elem.exlFloatValue.setnan ();
          }
          break;
        default:  // ?|=y
          elem.exlFloatValue.setnan ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "|=");
      }
    },

    //  文字列代入演算子
    ETY_OPERATOR_ASSIGN_STRING_TO_VARIABLE {  // v=y 変数への文字列単純代入
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = elem.exlParamX.exlParamX.exlStringValue = elem.exlParamY.exlEval (mode).exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "=");
      }
    },

    ETY_OPERATOR_CONCAT_STRING_TO_VARIABLE {  // v+=y 変数への文字列連結複合代入
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlStringValue = elem.exlParamX.exlParamX.exlStringValue += elem.exlParamY.exlEval (mode).exlStringValue;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "+=");
      }
    },

    ETY_OPERATOR_ASSIGN_STRING_TO_MEMORY {  // [a]=y メモリへの文字列単純代入
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        ExpressionElement valueA = elem.exlParamX.exlParamX.exlEval (mode);
        ExpressionElement valueY = elem.exlParamY.exlEval (mode);
        int a = valueA.exlFloatValue.geti ();
        int f = valueA.exlType == ElementType.ETY_OPERATOR_AT ? valueA.exlParamY.exlFloatValue.geti () : -1;
        elem.exlStringValue = MC68060.mmuPokeStringZ (a, valueY.exlStringValue, f);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "=");
      }
    },

    ETY_OPERATOR_CONCAT_STRING_TO_MEMORY {  // [a]+=y メモリへの文字列連結複合代入
      @Override protected int etyPriority () {
        return EPY_PRIORITY_ASSIGNMENT;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        ExpressionElement valueA = elem.exlParamX.exlParamX.exlEval (mode);
        ExpressionElement valueY = elem.exlParamY.exlEval (mode);
        int a = valueA.exlFloatValue.geti ();
        int f = valueA.exlType == ElementType.ETY_OPERATOR_AT ? valueA.exlParamY.exlFloatValue.geti () : -1;
        elem.exlStringValue = MC68060.mmuPokeStringZ (a, MC68060.mmuPeekStringZ (a, f) + valueY.exlStringValue, f);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendAssignmentOperatorTo (sb, "+=");
      }
    },

    //コロン演算子
    ETY_OPERATOR_COLON {  // x:y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COLON;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ":");
      }
    },

    //コンマ演算子
    ETY_OPERATOR_COMMA {  // x,y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMA;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlParamX.exlEval (mode);
        elem.exlParamY.exlEval (mode);
        if (elem.exlParamY.exlType == ElementType.ETY_FLOAT) {
          //elem.exlType = ElementType.ETY_FLOAT;
          elem.exlFloatValue.sete (elem.exlParamY.exlFloatValue);
        } else if (elem.exlParamY.exlType == ElementType.ETY_STRING) {
          //elem.exlType = ElementType.ETY_STRING;
          elem.exlStringValue = elem.exlParamY.exlStringValue;
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ",");
      }
    },

    //コマンド

    //アセンブル
    //  a <開始アドレス>
    ETY_COMMAND_ASSEMBLE {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (DebugConsole.dgtAssemblePC == 0) {
          DebugConsole.dgtAssemblePC = XEiJ.regPC;
        }
        if (elem.exlParamX != null) {
          ExpressionElement[] paramList = elem.exlParamX.exlEvalCommaList (mode);
          if (0 < paramList.length) {
            ExpressionElement param = paramList[0];
            if (param.exlType == ElementType.ETY_OPERATOR_AT) {  //a x@y
              DebugConsole.dgtAssemblePC = param.exlParamX.exlFloatValue.geti ();
              DebugConsole.dgtAssembleFC = param.exlParamY.exlFloatValue.geti ();
            } else if (param.exlValueType == ElementType.ETY_FLOAT) {  //a x
              DebugConsole.dgtAssemblePC = param.exlFloatValue.geti ();
              DebugConsole.dgtAssembleFC = XEiJ.regSRS == 0 ? 2 : 6;
            }
          }
        }
        //プロンプトを作る
        DebugConsole.dgtMakeAssemblerPrompt ();
        //アセンブラモードに移行する
        DebugConsole.dgtPrint (Multilingual.mlnJapanese ?
                               "[ . で終了]\n" :
                               "[enter . to exit]\n");
        DebugConsole.dgtInputMode = DebugConsole.DGT_INPUT_MODE_ASSEMBLER;
        DebugConsole.dgtPrintPrompt ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        sb.append ("a");
        if (elem.exlParamX != null) {
          elem.exlParamX.exlAppendTo (sb.append (' '));
        }
        return sb;
      }
    },

    //メモリダンプ
    //  d<サイズ> <開始アドレス>,<終了アドレス¹>
    //  ¹終了アドレスは範囲に含まれる
    ETY_COMMAND_DUMP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int size = elem.exlSubscript;  //サイズ
        //サイズ毎の準備
        String header;  //ヘッダの文字列
        int lineLength;  //1行のバイト数
        int groupLength;  //グループのバイト数。グループを" "で区切る。4グループ毎に"  "で区切る
        EFPBox.EFP tempF = null;  //浮動小数点数を入れる場所
        //                 1111111111222222222233333333334444444444555555555566666666667777777777
        //       01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //        address  +0 +1 +2 +3  +4 +5 +6 +7  +8 +9 +A +B  +C +D +E +F  |0123456789ABCDEF|
        //    b: aaaaaaaa  dd dd dd dd  dd dd dd dd  dd dd dd dd  dd dd dd dd  |................|
        //        address  +0+1 +2+3 +4+5 +6+7  +8+9 +A+B +C+D +E+F  |0123456789ABCDEF|
        //    w: aaaaaaaa  dddd dddd dddd dddd  dddd dddd dddd dddd  |................|
        //        address  +0+1+2+3 +4+5+6+7 +8+9+A+B +C+D+E+F  |0123456789ABCDEF|
        //    l: aaaaaaaa  dddddddd dddddddd dddddddd dddddddd  |................|
        //        address  +0+1+2+3+4+5+6+7 +8+9+A+B+C+D+E+F  |0123456789ABCDEF|
        //    q: aaaaaaaa  dddddddddddddddd dddddddddddddddd  |................|
        //        address  +0+1+2+3
        //    s: aaaaaaaa  dddddddd  +n.nnnnnnnnnnnnnnnnnnnnnnnnne+nnnnn
        //        address  +0+1+2+3+4+5+6+7
        //    d: aaaaaaaa  dddddddddddddddd  +n.nnnnnnnnnnnnnnnnnnnnnnnnne+nnnnn
        //        address  +0+1+2+3+4+5+6+7+8+9+A+B
        //  xtp: aaaaaaaa  dddddddddddddddddddddddd  +n.nnnnnnnnnnnnnnnnnnnnnnnnne+nnnnn
        switch (size) {
        case 'b':
          header = " address  +0 +1 +2 +3  +4 +5 +6 +7  +8 +9 +A +B  +C +D +E +F  |0123456789ABCDEF|\n";
          lineLength = 16;
          groupLength = 1;
          break;
        case 'w':
          header = " address  +0+1 +2+3 +4+5 +6+7  +8+9 +A+B +C+D +E+F  |0123456789ABCDEF|\n";
          lineLength = 16;
          groupLength = 2;
          break;
        case 'l':
          header = " address  +0+1+2+3 +4+5+6+7 +8+9+A+B +C+D+E+F  |0123456789ABCDEF|\n";
          lineLength = 16;
          groupLength = 4;
          break;
        case 'q':
          header = " address  +0+1+2+3+4+5+6+7 +8+9+A+B+C+D+E+F  |0123456789ABCDEF|\n";
          lineLength = 16;
          groupLength = 8;
          break;
        case 's':
          header = " address  +0+1+2+3\n";
          lineLength = 4;
          groupLength = 4;
          tempF = XEiJ.fpuBox.new EFP ();
          break;
        case 'd':
          header = " address  +0+1+2+3+4+5+6+7\n";
          lineLength = 8;
          groupLength = 8;
          tempF = XEiJ.fpuBox.new EFP ();
          break;
        case 'x':
        case 't':
        case 'p':
          header = " address  +0+1+2+3+4+5+6+7+8+9+A+B\n";
          lineLength = 12;
          groupLength = 12;
          tempF = XEiJ.fpuBox.new EFP ();
          break;
        default:
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "サイズが間違っています" :
                                   "wrong size");
          return;
        }
        //引数を評価する
        ExpressionElement[] paramArray = elem.exlParamX == null ? null : elem.exlParamX.exlEvalCommaList (mode);
        //開始アドレスを取り出す
        int startAddress;  //開始アドレス
        int functionCode;  //ファンクションコード
        int straddleChar;  //2行に跨る文字
        if (paramArray == null || paramArray.length < 1) {  //指定されていない
          //開始アドレスを復元する
          startAddress = DebugConsole.dgtDumpAddress;
          functionCode = DebugConsole.dgtDumpFunctionCode;
          straddleChar = DebugConsole.dgtDumpStraddleChar;
        } else {  //指定されている
          ExpressionElement param0 = paramArray[0];
          if (param0.exlValueType != ElementType.ETY_FLOAT) {
            DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                     "開始アドレスが間違っています" :
                                     "wrong start address");
            return;
          }
          if (param0.exlType == ElementType.ETY_OPERATOR_AT) {  // x@y
            startAddress = param0.exlParamX.exlFloatValue.geti32 ();
            functionCode = param0.exlParamY.exlFloatValue.geti32 () & 7;
          } else {
            startAddress = param0.exlFloatValue.geti32 ();
            functionCode = XEiJ.regSRS == 0 ? 1 : 5;
          }
          straddleChar = 0;
        }
        //終了アドレスを取り出す
        int endAddress;  //終了アドレス
        if (paramArray == null || paramArray.length < 2) {  //指定されていない
          endAddress = startAddress + lineLength * 16 - 1;  //16行
        } else {  //指定されている
          ExpressionElement param1 = paramArray[1];
          if (param1.exlValueType != ElementType.ETY_FLOAT) {
            DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                     "終了アドレスが間違っています" :
                                     "wrong end address");
            return;
          }
          endAddress = param1.exlFloatValue.geti32 ();
        }
        if ((endAddress - startAddress) < 0) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "アドレスが間違っています" :
                                   "wrong address");
          return;
        }
        //データのバッファを作る
        int byteCount = lineLength + (tempF == null ? 1 : 0);  //整数のとき1バイト余分に取り出す
        byte[] byteArray = new byte[byteCount];
        //ヘッダを出力する
        StringBuilder sb = new StringBuilder ();
        sb.append (header);
        //行ループ
        for (int a = startAddress; a <= endAddress; a += lineLength) {  //行の先頭アドレス
          //アドレスを出力する
          XEiJ.fmtHex8 (sb, a);
          //データを取り出す
          for (int i = 0; i < byteCount; i++) {
            if (a + i <= endAddress) {
              byteArray[i] = MC68060.mmuPeekByteSign (a + i, functionCode);
            } else {
              byteArray[i] = 0;
            }
          }
          //データを出力する
          for (int i = 0; i < lineLength; i++) {
            if (i % (groupLength * 4) == 0) {
              sb.append ("  ");
            } else if (i % groupLength == 0) {
              sb.append (" ");
            }
            if (a + i <= endAddress) {
              XEiJ.fmtHex2 (sb, byteArray[i]);
            } else {
              sb.append ("  ");
            }
          }
          sb.append ("  ");
          if (tempF == null) {  //整数のとき
            //文字を出力する
            int iStart;
            if (straddleChar == 0) {  //直前の行から跨る文字はない
              sb.append ('|');
              iStart = 0;
            } else {  //直前の行から跨る文字がある
              sb.append ((char) straddleChar);
              iStart = 1;
            }
            straddleChar = 0;
            for (int i = iStart; i < lineLength; i++) {
              int h = byteArray[i] & 255;
              int l = i + 1 < lineLength ? byteArray[i + 1] & 255 : 0;
              int c;
              if (((0x81 <= h && h <= 0x9f) || (0xe0 <= h && h <= 0xef)) &&
                  ((0x40 <= l && l <= 0x7e) || (0x80 <= l && l <= 0xfc))) {  //SJISの2バイトコード
                c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
                if (c == 0) {  //対応する文字がない
                  c = a + i <= endAddress ? '※' : '　';  //hが範囲外のとき空白にする
                }
                i++;
                if (i == lineLength) {  //2行に跨る
                  straddleChar = c;
                }
              } else {
                c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
                if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
                  c = a + i <= endAddress ? '.' : ' ';  //hが範囲外のとき空白にする
                }
              }
              sb.append ((char) c);
            }
            if (straddleChar == 0) {
              sb.append ('|');
            }
          } else {  //浮動小数点数のとき
            //浮動小数点数を出力する
            switch (size) {
            case 's':
              sb.append (tempF.setf0 (byteArray, 0).toString ());
              break;
            case 'd':
              sb.append (tempF.setd01 (byteArray, 0).toString ());
              break;
            case 'x':
              sb.append (tempF.setx012 (byteArray, 0).toString ());
              break;
            case 't':
              sb.append (tempF.sety012 (byteArray, 0).toString ());
              break;
            case 'p':
              sb.append (tempF.setp012 (byteArray, 0).toString ());
              break;
            }
            straddleChar = 0;
          }
          sb.append ('\n');
        }  //for a
        DebugConsole.dgtPrint (sb.toString ());
        //開始アドレスを保存する
        DebugConsole.dgtDumpAddress = endAddress + 1;
        DebugConsole.dgtDumpFunctionCode = functionCode;
        DebugConsole.dgtDumpStraddleChar = straddleChar;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("d").append ((char) elem.exlSubscript);
      }
    },

    //メモリ充填
    //  f<サイズ> <開始アドレス>,<終了アドレス¹>,<データ>,…
    //  ¹終了アドレスは範囲に含まれる
    ETY_COMMAND_FILL {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int size = elem.exlSubscript;  //サイズ
        //引数を評価する
        ExpressionElement[] paramArray = elem.exlParamX.exlEvalCommaList (mode);
        //開始アドレスを取り出す
        int startAddress;  //開始アドレス
        int functionCode;  //ファンクションコード
        ExpressionElement param0 = paramArray[0];
        if (param0.exlValueType != ElementType.ETY_FLOAT) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "開始アドレスが間違っています" :
                                   "wrong start address");
          return;
        }
        if (param0.exlType == ElementType.ETY_OPERATOR_AT) {  // x@y
          startAddress = param0.exlParamX.exlFloatValue.geti32 ();
          functionCode = param0.exlParamY.exlFloatValue.geti32 () & 7;
        } else {
          startAddress = param0.exlFloatValue.geti32 ();
          functionCode = XEiJ.regSRS == 0 ? 1 : 5;
        }
        //終了アドレスを取り出す
        int endAddress;  //終了アドレス
        ExpressionElement param1 = paramArray[1];
        if (param1.exlValueType != ElementType.ETY_FLOAT) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "終了アドレスが間違っています" :
                                   "wrong end address");
          return;
        }
        endAddress = param1.exlFloatValue.geti32 ();
        //データを取り出す
        int dataCount = paramArray.length - 2;  //データの数。1個以上あることはパーサで確認済み
        int byteCount;  //データのバイト数
        byte[] byteArray;  //データのバイト配列
        if (dataCount == 1 &&  //引数が1個で
            size == 'b' &&  //バイトサイズで
            paramArray[2].exlValueType == ElementType.ETY_STRING) {  //文字列のとき
          //文字列をbyte[]に変換する
          String s = paramArray[2].exlStringValue;
          byteArray = new byte[2 * s.length ()];
          byteCount = ByteArray.byaWstr (byteArray, 0, s);  //SJISに変換する
        } else {
          //データをEFP[]に変換する
          EFPBox.EFP[] dataArray = new EFPBox.EFP[dataCount];  //データの配列
          for (int i = 0; i < dataCount; i++) {
            ExpressionElement param = paramArray[2 + i];
            if (param.exlValueType != ElementType.ETY_FLOAT) {
              DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                       "データが間違っています" :
                                       "wrong data");
              return;
            }
            dataArray[i] = param.exlFloatValue;
          }
          //EFP[]をbyte[]に変換する
          switch (size) {
          case 'b':  //byte
            byteCount = 1 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              byteArray[i] = (byte) dataArray[i].geti32 ();
            }
            break;
          case 'w':  //word
            byteCount = 2 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              int t = dataArray[i].geti32 ();
              byteArray[2 * i] = (byte) (t >> 8);
              byteArray[2 * i + 1] = (byte) t;
            }
            break;
          case 'l':  //long
            byteCount = 4 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              int t = dataArray[i].geti32 ();
              byteArray[4 * i] = (byte) (t >> 24);
              byteArray[4 * i + 1] = (byte) (t >> 16);
              byteArray[4 * i + 2] = (byte) (t >> 8);
              byteArray[4 * i + 3] = (byte) t;
            }
            break;
          case 'q':  //quad
            byteCount = 8 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              long t = dataArray[i].geti64 ();
              byteArray[8 * i] = (byte) (t >> 56);
              byteArray[8 * i + 1] = (byte) (t >> 48);
              byteArray[8 * i + 2] = (byte) (t >> 40);
              byteArray[8 * i + 3] = (byte) (t >> 32);
              byteArray[8 * i + 4] = (byte) (t >> 24);
              byteArray[8 * i + 5] = (byte) (t >> 16);
              byteArray[8 * i + 6] = (byte) (t >> 8);
              byteArray[8 * i + 7] = (byte) t;
            }
            break;
          case 's':  //single
            byteCount = 4 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getf0 (byteArray, 4 * i);
            }
            break;
          case 'd':  //double
            byteCount = 8 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getd01 (byteArray, 8 * i);
            }
            break;
          case 'x':  //extended
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getx012 (byteArray, 12 * i);
            }
            break;
          case 't':  //triple
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].gety012 (byteArray, 12 * i);
            }
            break;
          case 'p':  //packed
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getp012 (byteArray, 12 * i);
            }
            break;
          default:
            DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                     "サイズが間違っています" :
                                     "wrong size");
            return;
          }
        }
        //byte[]で充填する
        for (int a = startAddress; a <= endAddress; a += byteCount) {
          for (int i = 0; i < byteCount; i++) {
            if (a + i <= endAddress) {
              MC68060.mmuPokeByte (a + i, byteArray[i], functionCode);
            }
          }
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("f").append ((char) elem.exlSubscript);
      }
    },

    //実行
    //  g <開始アドレス>
    ETY_COMMAND_RUN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.mpuTask != null) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "MPU が動作しています" :
                                   "MPU is running");
          return;
        }
        if (elem.exlParamX != null) {
          //引数を評価する
          ExpressionElement[] paramArray = elem.exlParamX.exlEvalCommaList (mode);
          if (0 < paramArray.length) {
            ExpressionElement param0 = paramArray[0];
            if (param0.exlValueType == ElementType.ETY_FLOAT) {  // x
              //!!! 分岐ログを修正する必要がある
              XEiJ.regPC = param0.exlFloatValue.geti32 ();
            }
          }
        }
        XEiJ.mpuStart ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        sb.append ("g");
        if (elem.exlParamX != null) {
          elem.exlParamX.exlAppendTo (sb.append (' '));
        }
        return sb;
      }
    },

    //ヘルプ
    //  h
    ETY_COMMAND_HELP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        DebugConsole.dgtPageList = new LinkedList<String> (
          Arrays.asList (Multilingual.mlnJapanese ? EVX_HELP_MESSAGE_JA : EVX_HELP_MESSAGE_EN));
        DebugConsole.dgtPrintPage ();
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("h");
      }
    },

    //停止
    //  i
    ETY_COMMAND_STOP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.mpuTask == null) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "MPU は停止しています" :
                                   "MPU is not running");
          return;
        }
        if (RootPointerList.RTL_ON) {
          if (RootPointerList.rtlCurrentSupervisorTaskIsStoppable ||
              RootPointerList.rtlCurrentUserTaskIsStoppable) {
            DebugConsole.dgtRequestRegs = 5;
            XEiJ.mpuStop (null);
          }
        } else {
          DebugConsole.dgtRequestRegs = 5;
          XEiJ.mpuStop (null);
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("i");
      }
    },

    //逆アセンブル
    //  l <開始>,<終了>
    ETY_COMMAND_LIST {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (DebugConsole.dgtDisassemblePC == 0) {
          DebugConsole.dgtDisassemblePC = XEiJ.regPC;
          DebugConsole.dgtDisassembleLastTail = XEiJ.regPC + 31 & -32;
        }
        int headAddress = DebugConsole.dgtDisassemblePC;
        int tailAddress = DebugConsole.dgtDisassembleLastTail + 32;  //前回の終了位置+32
        if (elem.exlParamX != null) {
          ExpressionElement[] list = elem.exlParamX.exlEvalCommaList (mode);
          if (0 < list.length) {
            ExpressionElement param = list[0];
            if (param.exlType == ElementType.ETY_OPERATOR_AT) {  //list x@y
              headAddress = DebugConsole.dgtDisassemblePC = param.exlParamX.exlFloatValue.geti32 ();
              tailAddress = headAddress + 63 & -32;  //32バイト以上先の32バイト境界
              DebugConsole.dgtDisassembleFC = param.exlParamY.exlFloatValue.geti32 ();
            } else if (param.exlValueType == ElementType.ETY_FLOAT) {  //list x
              headAddress = DebugConsole.dgtDisassemblePC = param.exlFloatValue.geti32 ();
              tailAddress = headAddress + 63 & -32;  //32バイト以上先の32バイト境界
              DebugConsole.dgtDisassembleFC = XEiJ.regSRS == 0 ? 2 : 6;
            }
            if (1 < list.length) {
              param = list[1];  //終了アドレス
              if (param.exlType == ElementType.ETY_OPERATOR_AT) {  // x@y
                tailAddress = param.exlParamX.exlFloatValue.geti32 ();
              } else if (param.exlValueType == ElementType.ETY_FLOAT) {  // x
                tailAddress = param.exlFloatValue.geti32 ();
              }
              if (Integer.compareUnsigned (tailAddress, headAddress) < 0) {  //終了アドレス<開始アドレス
                tailAddress = headAddress;  //少なくとも1ワードは逆アセンブルする
              }
              tailAddress = tailAddress + 2 & -2;  //終了アドレスの次の偶数境界
            }
          }
        }
        int supervisor = DebugConsole.dgtDisassembleFC & 4;
        //ラベルの準備
        //LabeledAddress.lblUpdateProgram ();
        boolean prevBranchFlag = false;  //true=直前が完全分岐命令だった
        //命令ループ
        StringBuilder sb = new StringBuilder ();
        int itemAddress = headAddress;
        int itemEndAddress;
        do {
          //完全分岐命令の下に隙間を空けて読みやすくする
          if (prevBranchFlag) {
            sb.append ('\n');
            //ラベル
            int l = sb.length ();
            LabeledAddress.lblSearch (sb, itemAddress);
            if (l < sb.length ()) {
              sb.append ('\n');
            }
          }
          //逆アセンブルする
          String code = Disassembler.disDisassemble (new StringBuilder (), itemAddress, supervisor).toString ();
          itemEndAddress = Disassembler.disPC;
          //1行目
          int lineAddress = itemAddress;  //行の開始アドレス
          int lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
          //アドレス
          XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
          //データ
          for (int a = lineAddress; a < lineEndAddress; a += 2) {
            XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
          }
          sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + 10 - lineEndAddress) + 2);
          //逆アセンブル結果
          sb.append (code).append (XEiJ.DBG_SPACES, 0, Math.max (1, 68 - 32 - code.length ()));
          //キャラクタ
          for (int a = lineAddress; a < lineEndAddress; a++) {
            int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
            int c;
            if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
              int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
              if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
                c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
                if (c == 0) {  //対応する文字がない
                  c = '※';
                }
                a++;
              } else {  //SJISの2バイトコードの2バイト目ではない
                c = '.';  //SJISの2バイトコードの1バイト目ではなかった
              }
            } else {  //SJISの2バイトコードの1バイト目ではない
              c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
              if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
                c = '.';
              }
            }
            sb.append ((char) c);
          }  //for a
          sb.append ('\n');
          //2行目以降
          while (lineEndAddress < itemEndAddress) {
            lineAddress = lineEndAddress;  //行の開始アドレス
            lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
            //アドレス
            XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
            //データ
            for (int a = lineAddress; a < lineEndAddress; a += 2) {
              XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
            }
            sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + 10 - lineEndAddress) + (2 + 68 - 32));
            //キャラクタ
            for (int a = lineAddress; a < lineEndAddress; a++) {
              int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
              int c;
              if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
                int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
                if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
                  c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
                  if (c == 0) {  //対応する文字がない
                    c = '※';
                  }
                  a++;
                } else {  //SJISの2バイトコードの2バイト目ではない
                  c = '.';  //SJISの2バイトコードの1バイト目ではなかった
                }
              } else {  //SJISの2バイトコードの1バイト目ではない
                c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
                if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
                  c = '.';
                }
              }
              sb.append ((char) c);
            }  //for a
            sb.append ('\n');
          }  //while
          //完全分岐命令の下に隙間を空けて読みやすくする
          prevBranchFlag = (Disassembler.disStatus & Disassembler.DIS_ALWAYS_BRANCH) != 0;
          itemAddress = itemEndAddress;
        } while (itemAddress - headAddress < tailAddress - headAddress);
        DebugConsole.dgtPrint (sb.toString ());
        DebugConsole.dgtDisassemblePC = itemEndAddress;
        DebugConsole.dgtDisassembleLastTail = tailAddress;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        sb.append ("l");
        if (elem.exlParamX != null) {
          elem.exlParamX.exlAppendTo (sb.append (' '));
        }
        return sb;
      }
    },

    //ラベル一覧
    //  ll
    ETY_COMMAND_LABEL_LIST {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        DebugConsole.dgtPrint (LabeledAddress.lblDump ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("ll");
      }
    },

    //メモリ編集
    //  me<サイズ> <開始アドレス>,<データ>,…
    ETY_COMMAND_MEMORY_EDIT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int size = elem.exlSubscript;  //サイズ
        //引数を評価する
        ExpressionElement[] paramArray = elem.exlParamX.exlEvalCommaList (mode);
        //開始アドレスを取り出す
        int startAddress;  //開始アドレス
        int functionCode;  //ファンクションコード
        ExpressionElement param0 = paramArray[0];
        if (param0.exlValueType != ElementType.ETY_FLOAT) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "開始アドレスが間違っています" :
                                   "wrong start address");
          return;
        }
        if (param0.exlType == ElementType.ETY_OPERATOR_AT) {  // x@y
          startAddress = param0.exlParamX.exlFloatValue.geti32 ();
          functionCode = param0.exlParamY.exlFloatValue.geti32 () & 7;
        } else {
          startAddress = param0.exlFloatValue.geti32 ();
          functionCode = XEiJ.regSRS == 0 ? 1 : 5;
        }
        //データを取り出す
        int dataCount = paramArray.length - 1;  //データの数。1個以上あることはパーサで確認済み
        int byteCount;  //データのバイト数
        byte[] byteArray;  //データのバイト配列
        if (dataCount == 1 &&  //引数が1個で
            size == 'b' &&  //バイトサイズで
            paramArray[1].exlValueType == ElementType.ETY_STRING) {  //文字列のとき
          //文字列をbyte[]に変換する
          String s = paramArray[1].exlStringValue;
          byteArray = new byte[2 * s.length ()];
          byteCount = ByteArray.byaWstr (byteArray, 0, s);  //SJISに変換する
        } else {
          //データをEFP[]に変換する
          EFPBox.EFP[] dataArray = new EFPBox.EFP[dataCount];  //データの配列
          for (int i = 0; i < dataCount; i++) {
            ExpressionElement param = paramArray[1 + i];
            if (param.exlValueType != ElementType.ETY_FLOAT) {
              DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                       "データが間違っています" :
                                       "wrong data");
              return;
            }
            dataArray[i] = param.exlFloatValue;
          }
          //EFP[]をbyte[]に変換する
          switch (size) {
          case 'b':  //byte
            byteCount = 1 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              byteArray[i] = (byte) dataArray[i].geti32 ();
            }
            break;
          case 'w':  //word
            byteCount = 2 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              int t = dataArray[i].geti32 ();
              byteArray[2 * i] = (byte) (t >> 8);
              byteArray[2 * i + 1] = (byte) t;
            }
            break;
          case 'l':  //long
            byteCount = 4 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              int t = dataArray[i].geti32 ();
              byteArray[4 * i] = (byte) (t >> 24);
              byteArray[4 * i + 1] = (byte) (t >> 16);
              byteArray[4 * i + 2] = (byte) (t >> 8);
              byteArray[4 * i + 3] = (byte) t;
            }
            break;
          case 'q':  //quad
            byteCount = 8 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              long t = dataArray[i].geti64 ();
              byteArray[8 * i] = (byte) (t >> 56);
              byteArray[8 * i + 1] = (byte) (t >> 48);
              byteArray[8 * i + 2] = (byte) (t >> 40);
              byteArray[8 * i + 3] = (byte) (t >> 32);
              byteArray[8 * i + 4] = (byte) (t >> 24);
              byteArray[8 * i + 5] = (byte) (t >> 16);
              byteArray[8 * i + 6] = (byte) (t >> 8);
              byteArray[8 * i + 7] = (byte) t;
            }
            break;
          case 's':  //single
            byteCount = 4 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getf0 (byteArray, 4 * i);
            }
            break;
          case 'd':  //double
            byteCount = 8 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getd01 (byteArray, 8 * i);
            }
            break;
          case 'x':  //extended
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getx012 (byteArray, 12 * i);
            }
            break;
          case 't':  //triple
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].gety012 (byteArray, 12 * i);
            }
            break;
          case 'p':  //packed
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getp012 (byteArray, 12 * i);
            }
            break;
          default:
            DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                     "サイズが間違っています" :
                                     "wrong size");
            return;
          }
        }
        //byte[]を書き込む
        for (int i = 0; i < byteCount; i++) {
          MC68060.mmuPokeByte (startAddress + i, byteArray[i], functionCode);
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("me").append ((char) elem.exlSubscript);
      }
    },

    //メモリ検索
    //  ms<サイズ> <開始アドレス>,<終了アドレス¹>,<データ>,…
    //  ¹終了アドレスは範囲に含まれる
    ETY_COMMAND_MEMORY_SEARCH {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        int size = elem.exlSubscript;  //サイズ
        //引数を評価する
        ExpressionElement[] paramArray = elem.exlParamX.exlEvalCommaList (mode);
        //開始アドレスを取り出す
        int startAddress;  //開始アドレス
        int functionCode;  //ファンクションコード
        ExpressionElement param0 = paramArray[0];
        if (param0.exlValueType != ElementType.ETY_FLOAT) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "開始アドレスが間違っています" :
                                   "wrong start address");
          return;
        }
        if (param0.exlType == ElementType.ETY_OPERATOR_AT) {  // x@y
          startAddress = param0.exlParamX.exlFloatValue.geti32 ();
          functionCode = param0.exlParamY.exlFloatValue.geti32 () & 7;
        } else {
          startAddress = param0.exlFloatValue.geti32 ();
          functionCode = XEiJ.regSRS == 0 ? 1 : 5;
        }
        //終了アドレスを取り出す
        int endAddress;  //終了アドレス
        ExpressionElement param1 = paramArray[1];
        if (param1.exlType == ElementType.ETY_FLOAT) {  // x
          endAddress = param1.exlFloatValue.geti32 ();
        } else {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "終了アドレスが間違っています" :
                                   "wrong end address");
          return;
        }
        //データを取り出す
        int dataCount = paramArray.length - 2;  //データの数。1個以上あることはパーサで確認済み
        int byteCount;  //データのバイト数
        byte[] byteArray;  //データのバイト配列
        if (dataCount == 1 &&  //引数が1個で
            size == 'b' &&  //バイトサイズで
            paramArray[2].exlValueType == ElementType.ETY_STRING) {  //文字列のとき
          //文字列をbyte[]に変換する
          String s = paramArray[2].exlStringValue;
          byteArray = new byte[2 * s.length ()];
          byteCount = ByteArray.byaWstr (byteArray, 0, s);  //SJISに変換する
        } else {
          //データをEFP[]に変換する
          EFPBox.EFP[] dataArray = new EFPBox.EFP[dataCount];  //データの配列
          for (int i = 0; i < dataCount; i++) {
            ExpressionElement param = paramArray[2 + i];
            if (param.exlValueType != ElementType.ETY_FLOAT) {
              DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                       "データが間違っています" :
                                       "wrong data");
              return;
            }
            dataArray[i] = param.exlFloatValue;
          }
          //EFP[]をbyte[]に変換する
          switch (size) {
          case 'b':  //byte
            byteCount = 1 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              byteArray[i] = (byte) dataArray[i].geti32 ();
            }
            break;
          case 'w':  //word
            byteCount = 2 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              int t = dataArray[i].geti32 ();
              byteArray[2 * i] = (byte) (t >> 8);
              byteArray[2 * i + 1] = (byte) t;
            }
            break;
          case 'l':  //long
            byteCount = 4 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              int t = dataArray[i].geti32 ();
              byteArray[4 * i] = (byte) (t >> 24);
              byteArray[4 * i + 1] = (byte) (t >> 16);
              byteArray[4 * i + 2] = (byte) (t >> 8);
              byteArray[4 * i + 3] = (byte) t;
            }
            break;
          case 'q':  //quad
            byteCount = 8 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              long t = dataArray[i].geti64 ();
              byteArray[8 * i] = (byte) (t >> 56);
              byteArray[8 * i + 1] = (byte) (t >> 48);
              byteArray[8 * i + 2] = (byte) (t >> 40);
              byteArray[8 * i + 3] = (byte) (t >> 32);
              byteArray[8 * i + 4] = (byte) (t >> 24);
              byteArray[8 * i + 5] = (byte) (t >> 16);
              byteArray[8 * i + 6] = (byte) (t >> 8);
              byteArray[8 * i + 7] = (byte) t;
            }
            break;
          case 's':  //single
            byteCount = 4 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getf0 (byteArray, 4 * i);
            }
            break;
          case 'd':  //double
            byteCount = 8 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getd01 (byteArray, 8 * i);
            }
            break;
          case 'x':  //extended
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getx012 (byteArray, 12 * i);
            }
            break;
          case 't':  //triple
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].gety012 (byteArray, 12 * i);
            }
            break;
          case 'p':  //packed
            byteCount = 12 * dataCount;
            byteArray = new byte[byteCount];
            for (int i = 0; i < dataCount; i++) {
              dataArray[i].getp012 (byteArray, 12 * i);
            }
            break;
          default:
            DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                     "サイズが間違っています" :
                                     "wrong size");
            return;
          }
        }
        if (byteCount <= 0) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "データがありません" :
                                   "no data");
          return;
        }
        //byte[]を検索する
        StringBuilder sb = new StringBuilder ();
        int count = 0;
      for_a:
        for (int a = startAddress; a <= endAddress + 1 - byteCount; a++) {
          for (int i = 0; i < byteCount; i++) {
            if (MC68060.mmuPeekByteSign (a + i, functionCode) != byteArray[i]) {
              continue for_a;
            }
          }
          XEiJ.fmtHex8 (sb, a);
          sb.append ('\n');
          count++;
          if (count == 1000) {
            break;
          }
        }
        DebugConsole.dgtPrint (sb.toString ());
        if (count == 0) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "見つかりません" :
                                   "not found");
        } else if (count < 1000) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   count + "個見つかりました" :
                                   count + " found");
        } else {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   count + "個以上見つかりました" :
                                   count + " or more found");
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("ms").append ((char) elem.exlSubscript);
      }
    },

    //計算結果表示
    //  p <式>,…
    ETY_COMMAND_PRINT {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (elem.exlParamX != null) {
          for (ExpressionElement param : elem.exlParamX.exlEvalCommaList (mode)) {
            param.exlPrint ();
          }
        }
        DebugConsole.dgtPrintChar ('\n');
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        sb.append ("p");
        if (elem.exlParamX != null) {
          elem.exlParamX.exlAppendTo (sb.append (' '));
        }
        return sb;
      }
    },

    //ステップアンティルリターン
    //  r
    ETY_COMMAND_RETURN {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.mpuTask == null) {
          DebugConsole.dgtRequestRegs = 5;
          XEiJ.mpuStepUntilReturn ();
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("r");
      }
    },

    //ステップ
    //  s <回数>
    ETY_COMMAND_STEP {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.mpuTask != null) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "MPU が動作しています" :
                                   "MPU is running");
          return;
        }
        int n = 1;  //回数
        if (elem.exlParamX != null) {
          ExpressionElement[] list = elem.exlParamX.exlEvalCommaList (mode);
          if (0 < list.length) {
            ExpressionElement param = list[0];
            if (param.exlValueType == ElementType.ETY_FLOAT) {
              n = Math.max (1, Math.min (1000, param.exlFloatValue.geti ()));
            }
          }
        }
        DebugConsole.dgtRequestRegs = 5;
        XEiJ.mpuStep (n);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("s");
      }
    },

    //トレース
    //  t <回数>
    ETY_COMMAND_TRACE {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.mpuTask != null) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "MPU が動作しています" :
                                   "MPU is running");
          return;
        }
        int n = 1;  //回数
        if (elem.exlParamX != null) {
          ExpressionElement[] list = elem.exlParamX.exlEvalCommaList (mode);
          if (0 < list.length) {
            ExpressionElement param = list[0];
            if (param.exlValueType == ElementType.ETY_FLOAT) {
              n = Math.max (1, Math.min (1000, param.exlFloatValue.geti ()));
            }
          }
        }
        DebugConsole.dgtRequestRegs = 5;
        XEiJ.mpuAdvance (n);
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("t");
      }
    },

    //レジスタ表示付きトレース
    //  tx <回数>
    ETY_COMMAND_TRACE_REGS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.mpuTask != null) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "MPU が動作しています" :
                                   "MPU is running");
          return;
        }
        int n = 1;  //回数
        if (elem.exlParamX != null) {
          ExpressionElement[] list = elem.exlParamX.exlEvalCommaList (mode);
          if (0 < list.length) {
            ExpressionElement param = list[0];
            if (param.exlValueType == ElementType.ETY_FLOAT) {
              n = Math.max (1, Math.min (1000, param.exlFloatValue.geti ()));
            }
          }
        }
        for (int i = 0; i < n; i++) {
          DebugConsole.dgtRequestRegs = i < n - 1 ? 1 : 5;  //最終回だけプロンプトを表示する
          XEiJ.mpuAdvance (1);
          //表示が完了するまで待つ。ステップは終了しない場合があるのでこの方法は使えない
          while (DebugConsole.dgtRequestRegs != 0) {
            try {
              Thread.sleep (1L);
            } catch (InterruptedException ie) {
            }
          }
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("tx");
      }
    },

    //浮動小数点レジスタ表示付きトレース
    //  txf <回数>
    ETY_COMMAND_TRACE_FLOAT_REGS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.mpuTask != null) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "MPU が動作しています" :
                                   "MPU is running");
          return;
        }
        int n = 1;  //回数
        if (elem.exlParamX != null) {
          ExpressionElement[] list = elem.exlParamX.exlEvalCommaList (mode);
          if (0 < list.length) {
            ExpressionElement param = list[0];
            if (param.exlValueType == ElementType.ETY_FLOAT) {
              n = Math.max (1, Math.min (1000, param.exlFloatValue.geti ()));
            }
          }
        }
        for (int i = 0; i < n; i++) {
          DebugConsole.dgtRequestRegs = i < n - 1 ? 3 : 7;  //最終回だけプロンプトを表示する
          XEiJ.mpuAdvance (1);
          //表示が完了するまで待つ。ステップは終了しない場合があるのでこの方法は使えない
          while (DebugConsole.dgtRequestRegs != 0) {
            try {
              Thread.sleep (1L);
            } catch (InterruptedException ie) {
            }
          }
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("txf");
      }
    },

    //レジスタ一覧
    //  x
    ETY_COMMAND_REGS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        StringBuilder sb = new StringBuilder ();
        //コアの動作中はレジスタの値が刻々と変化するのでpcとsrsはコピーしてから使う
        int pc = XEiJ.regPC;
        int srs = XEiJ.regSRS;
        //1行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "PC:xxxxxxxx USP:xxxxxxxx SSP:xxxxxxxx SR:xxxx  X:b N:b Z:b V:b C:b\n"
        //  または
        //  "PC:xxxxxxxx USP:xxxxxxxx ISP:xxxxxxxx MSP:xxxxxxxx SR:xxxx  X:b N:b Z:b V:b C:b\n"
        XEiJ.fmtHex8 (sb.append ("PC:"), pc);  //PC
        XEiJ.fmtHex8 (sb.append (" USP:"), srs != 0 ? XEiJ.mpuUSP : XEiJ.regRn[15]);  //USP
        if (XEiJ.currentMPU < Model.MPU_MC68020) {  //000
          XEiJ.fmtHex8 (sb.append (" SSP:"), srs != 0 ? XEiJ.regRn[15] : XEiJ.mpuISP);  //SSP
          XEiJ.fmtHex4 (sb.append (" SR:"), XEiJ.regSRT1 | srs | XEiJ.regSRI | XEiJ.regCCR);  //SR
        } else if (XEiJ.currentMPU < Model.MPU_MC68LC040) {  //030
          XEiJ.fmtHex8 (sb.append (" ISP:"), srs == 0 || XEiJ.regSRM != 0 ? XEiJ.mpuISP : XEiJ.regRn[15]);  //ISP
          XEiJ.fmtHex8 (sb.append (" MSP:"), srs == 0 || XEiJ.regSRM == 0 ? XEiJ.mpuMSP : XEiJ.regRn[15]);  //MSP
          XEiJ.fmtHex4 (sb.append (" SR:"), XEiJ.regSRT1 | XEiJ.regSRT0 | srs | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR);  //SR
        } else {  //060
          XEiJ.fmtHex8 (sb.append (" SSP:"), srs != 0 ? XEiJ.regRn[15] : XEiJ.mpuISP);  //SSP
          XEiJ.fmtHex4 (sb.append (" SR:"), XEiJ.regSRT1 | srs | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR);  //SR
        }
        sb.append ("  X:").append (XEiJ.REG_CCRXMAP[XEiJ.regCCR]);  //X
        sb.append (" N:").append (XEiJ.REG_CCRNMAP[XEiJ.regCCR]);  //N
        sb.append (" Z:").append (XEiJ.REG_CCRZMAP[XEiJ.regCCR]);  //Z
        sb.append (" V:").append (XEiJ.REG_CCRVMAP[XEiJ.regCCR]);  //V
        sb.append (" C:").append (XEiJ.REG_CCRCMAP[XEiJ.regCCR]);  //C
        sb.append ('\n');
        //2行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "HI:b LS:b CC(HS):b CS(LO):b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b\n"
        sb.append ("HI:").append (XEiJ.MPU_CCCMAP[ 2 << 5 | XEiJ.regCCR]);  //HI
        sb.append (" LS:").append (XEiJ.MPU_CCCMAP[ 3 << 5 | XEiJ.regCCR]);  //LS
        sb.append (" CC(HS):").append (XEiJ.MPU_CCCMAP[ 4 << 5 | XEiJ.regCCR]);  //CC(HS)
        sb.append (" CS(LO):").append (XEiJ.MPU_CCCMAP[ 5 << 5 | XEiJ.regCCR]);  //CS(LO)
        sb.append (" NE:").append (XEiJ.MPU_CCCMAP[ 6 << 5 | XEiJ.regCCR]);  //NE
        sb.append (" EQ:").append (XEiJ.MPU_CCCMAP[ 7 << 5 | XEiJ.regCCR]);  //EQ
        sb.append (" VC:").append (XEiJ.MPU_CCCMAP[ 8 << 5 | XEiJ.regCCR]);  //VC
        sb.append (" VS:").append (XEiJ.MPU_CCCMAP[ 9 << 5 | XEiJ.regCCR]);  //VS
        sb.append (" PL:").append (XEiJ.MPU_CCCMAP[10 << 5 | XEiJ.regCCR]);  //PL
        sb.append (" MI:").append (XEiJ.MPU_CCCMAP[11 << 5 | XEiJ.regCCR]);  //MI
        sb.append (" GE:").append (XEiJ.MPU_CCCMAP[12 << 5 | XEiJ.regCCR]);  //GE
        sb.append (" LT:").append (XEiJ.MPU_CCCMAP[13 << 5 | XEiJ.regCCR]);  //LT
        sb.append (" GT:").append (XEiJ.MPU_CCCMAP[14 << 5 | XEiJ.regCCR]);  //GT
        sb.append (" LE:").append (XEiJ.MPU_CCCMAP[15 << 5 | XEiJ.regCCR]);  //LE
        sb.append ('\n');
        //3～5行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "D0:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx D4:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n"
        //  "A0:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx A4:xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n"
        XEiJ.fmtHex8 (sb.append ("D0:") , XEiJ.regRn[ 0]);  //D0
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 1]);  //D1
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 2]);  //D2
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 3]);  //D3
        XEiJ.fmtHex8 (sb.append (" D4:"), XEiJ.regRn[ 4]);  //D4
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 5]);  //D5
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 6]);  //D6
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 7]);  //D7
        sb.append ('\n');
        XEiJ.fmtHex8 (sb.append ("A0:") , XEiJ.regRn[ 8]);  //A0
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[ 9]);  //A1
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[10]);  //A2
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[11]);  //A3
        XEiJ.fmtHex8 (sb.append (" A4:"), XEiJ.regRn[12]);  //A4
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[13]);  //A5
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[14]);  //A6
        XEiJ.fmtHex8 (sb.append (' ')   , XEiJ.regRn[15]);  //A7
        sb.append ('\n');
        if (Model.MPU_MC68020 <= XEiJ.currentMPU) {
          //6行目
          //             1111111111222222222233333333334444444444455555555566666666667777777777
          //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
          //  "SFC:x DFC:x VBR:xxxxxxxx CACR:xxxxxxxx  TCR:xxxxxxxx URP:xxxxxxxx SRP:xxxxxxxx\n"
          sb.append ("SFC:").append ((char) ('0' + XEiJ.mpuSFC));  //SFC
          sb.append (" DFC:").append ((char) ('0' + XEiJ.mpuDFC));  //DFC
          XEiJ.fmtHex8 (sb.append (" VBR:"), XEiJ.mpuVBR);  //VBR
          XEiJ.fmtHex8 (sb.append (" CACR:"), XEiJ.mpuCACR);  //CACR
          if (Model.MPU_MC68LC040 <= XEiJ.currentMPU) {
            XEiJ.fmtHex8 (sb.append ("  TCR:"), MC68060.mmuTCR);  //TCR
            XEiJ.fmtHex8 (sb.append (" URP:"), MC68060.mmuURP);  //URP
            XEiJ.fmtHex8 (sb.append (" SRP:"), MC68060.mmuSRP);  //SRP
          }
          sb.append ('\n');
          //7行目
          //             1111111111222222222233333333334444444444455555555566666666667777777777
          //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
          //  "ITT0:xxxxxxxx ITT1:xxxxxxxx DTT0:xxxxxxxx DTT1:xxxxxxxx  PCR:xxxxxxxx\n"
          if (Model.MPU_MC68LC040 <= XEiJ.currentMPU) {
            XEiJ.fmtHex8 (sb.append ("ITT0:"), MC68060.mmuITT0);  //ITT0
            XEiJ.fmtHex8 (sb.append (" ITT1:"), MC68060.mmuITT1);  //ITT1
            XEiJ.fmtHex8 (sb.append (" DTT0:"), MC68060.mmuDTT0);  //DTT0
            XEiJ.fmtHex8 (sb.append (" DTT1:"), MC68060.mmuDTT1);  //DTT1
            XEiJ.fmtHex8 (sb.append ("  PCR:"), XEiJ.mpuPCR);  //PCR
            sb.append ('\n');
          }
        }
        //ラベル
        //LabeledAddress.lblUpdateProgram ();
        {
          int l = sb.length ();
          LabeledAddress.lblSearch (sb, pc);
          if (l < sb.length ()) {
            sb.append ('\n');
          }
        }
        //逆アセンブルする
        String code = Disassembler.disDisassemble (new StringBuilder (), pc, srs).toString ();
        //アドレス
        XEiJ.fmtHex8 (sb, pc).append ("  ");
        //データ
        for (int a = pc; a < Disassembler.disPC; a += 2) {
          XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, srs));
        }
        for (int a = Disassembler.disPC; a < pc + 10; a += 2) {
          sb.append ("    ");
        }
        //コード
        sb.append ("  ").append (code).append ('\n');
        DebugConsole.dgtPrint (sb.toString ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("x");
      }
    },

    //浮動小数点レジスタ一覧
    //  xf
    ETY_COMMAND_FLOAT_REGS {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        if (XEiJ.currentMPU < Model.MPU_MC68020) {
          DebugConsole.dgtPrintln (Multilingual.mlnJapanese ?
                                   "浮動小数点レジスタはありません" :
                                   "no floating point register exists");
          return;
        }
        StringBuilder sb = new StringBuilder ();
        //8行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "FPCR:xxxxxxxx FPSR:xxxxxxxx  M:b Z:b I:b N:b  B:b S:b E:b O:b U:b D:b X:b P:b\n"
        XEiJ.fmtHex8 (sb.append ("FPCR:"), XEiJ.fpuBox.epbFpcr);  //FPCR
        XEiJ.fmtHex8 (sb.append (" FPSR:"), XEiJ.fpuBox.epbFpsr);  //FPSR
        sb.append ("  M:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 27 & 1)));  //FPSR M
        sb.append (" Z:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 26 & 1)));  //FPSR Z
        sb.append (" I:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 25 & 1)));  //FPSR I
        sb.append (" N:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 24 & 1)));  //FPSR N
        sb.append ("  B:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 15 & 1)));  //FPSR BSUN
        sb.append (" S:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 14 & 1)));  //FPSR SNAN
        sb.append (" E:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 13 & 1)));  //FPSR OPERR
        sb.append (" O:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 12 & 1)));  //FPSR OVFL
        sb.append (" U:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 11 & 1)));  //FPSR UNFL
        sb.append (" D:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >> 10 & 1)));  //FPSR DZ
        sb.append (" X:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >>  9 & 1)));  //FPSR INEX2
        sb.append (" P:").append ((char) ('0' + (XEiJ.fpuBox.epbFpsr >>  8 & 1)));  //FPSR INEX1
        sb.append ('\n');
        //9～12行目
        //             1111111111222222222233333333334444444444455555555566666666667777777777
        //   01234567890123456789012345678901234567890123456789012345678901234567890123456789
        //  "FP0:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP1:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
        //  "FP2:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP3:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
        //  "FP4:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP5:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
        //  "FP6:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx FP7:+x.xxxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n"
        for (int n = 0; n <= 7; n++) {
          String s = XEiJ.fpuFPn[n].toString ();
          sb.append ("FP").append (n).append (':').append (s);
          if ((n & 1) == 0) {
            sb.append (XEiJ.DBG_SPACES, 0, Math.max (0, 36 - s.length ()));
          } else {
            sb.append ('\n');
          }
        }
        DebugConsole.dgtPrint (sb.toString ());
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append ("xf");
      }
    },

    //ライン
    //          x  ラベル
    //     string  ニモニック
    //  subscript  オペレーションサイズ。-1,'b','w','l','q','s','d','x','p'のいずれか
    //          y  オペランド
    ETY_LINE {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_COMMAND;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        if (elem.exlParamX != null) {
          //ラベル
          elem.exlParamX.exlAppendTo (sb);
        }
        if (elem.exlParamX != null || elem.exlStringValue != null) {
          //空白
          sb.append (' ');
        }
        if (elem.exlStringValue != null) {
          //ニモニック
          sb.append (elem.exlStringValue);
          if (0 <= elem.exlSubscript) {
            //オペレーションサイズ
            sb.append ('.').append ((char) elem.exlSubscript);
          }
          if (elem.exlParamY != null) {
            //空白
            sb.append (' ');
            //オペランド
            elem.exlParamY.exlAppendTo (sb);
          }
        }
        return sb;
      }
    },

    //セパレータ
    //    separator(x=separator(x=line(x=label1,
    //                                 string=mnemonic1,
    //                                 subscript=size1,
    //                                 y=comma(x=comma(x=comma(x=operand11,
    //                                                         y=operand12),
    //                                                 y=operand13),
    //                                         y=operand14)),
    //                          y=line(x=label2,
    //                                 string=mnemonic2,
    //                                 subscript=size2,
    //                                 y=comma(x=comma(x=comma(x=operand21,
    //                                                         y=operand22),
    //                                                 y=operand23),
    //                                         y=operand24))),
    //              y=line(x=label3,
    //                     string=mnemonic3,
    //                     subscript=size3,
    //                     y=comma(x=comma(x=comma(x=operand31,
    //                                             y=operand32),
    //                                     y=operand33),
    //                             y=operand34)))
    ETY_SEPARATOR {  // x;y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_SEPARATOR;
      }
      @Override protected void etyEval (ExpressionElement elem, int mode) {
        elem.exlParamX.exlEval (mode);
        elem.exlParamY.exlEval (mode);
        if (elem.exlParamY.exlType == ElementType.ETY_FLOAT) {
          //elem.exlType = ElementType.ETY_FLOAT;
          elem.exlFloatValue.sete (elem.exlParamY.exlFloatValue);
        } else if (elem.exlParamY.exlType == ElementType.ETY_STRING) {
          //elem.exlType = ElementType.ETY_STRING;
          elem.exlStringValue = elem.exlParamY.exlStringValue;
        }
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, ";");
      }
    },

    //仮トークン
    //  パス1で機能を確定できないか、パス2で捨てられるトークン
    ETY_TOKEN_EXCLAMATION_MARK,  // !
    //ETY_TOKEN_QUOTATION_MARK,  // "
    ETY_TOKEN_NUMBER_SIGN,  // #
    //ETY_TOKEN_DOLLAR_SIGN,  // $
    ETY_TOKEN_PERCENT_SIGN,  // %
    //ETY_TOKEN_AMPERSAND,  // &
    //ETY_TOKEN_APOSTROPHE,  // '
    ETY_TOKEN_LEFT_PARENTHESIS,  // (
    ETY_TOKEN_RIGHT_PARENTHESIS,  // )
    ETY_TOKEN_ASTERISK,  // *
    ETY_TOKEN_PLUS_SIGN,  // +
    ETY_TOKEN_PLUS_PLUS,  // ++
    ETY_TOKEN_COMMA,  // ,
    ETY_TOKEN_HYPHEN_MINUS,  // -
    ETY_TOKEN_MINUS_MINUS,  // --
    ETY_TOKEN_FULL_STOP,  // .
    ETY_TOKEN_SOLIDUS,  // /
    //0-9
    ETY_TOKEN_COLON,  // :
    ETY_TOKEN_SEMICOLON,  // ;
    //ETY_TOKEN_LESS_THAN_SIGN,  // <
    //ETY_TOKEN_EQUALS_SIGN,  // =
    //ETY_TOKEN_GREATER_THAN_SIGN,  // >
    ETY_TOKEN_QUESTION_MARK,  // ?
    //ETY_TOKEN_COMMERCIAL_AT,  // @
    //A-Z
    ETY_TOKEN_LEFT_SQUARE_BRACKET,  // [
    //ETY_TOKEN_REVERSE_SOLIDUS,  // \\
    ETY_TOKEN_RIGHT_SQUARE_BRACKET,  // ]
    //ETY_TOKEN_CIRCUMFLEX_ACCENT,  // ^
    //_
    //ETY_TOKEN_GRAVE_ACCENT,  // `
    //a-z
    ETY_TOKEN_LEFT_CURLY_BRACKET,  // {
    //ETY_TOKEN_VERTICAL_LINE,  // |
    ETY_TOKEN_RIGHT_CURLY_BRACKET,  // }
    ETY_TOKEN_TILDE,  // ~

    //以下はアセンブラ用

    //サイズ
    //  ディスプレースメントまたはインデックスレジスタのサイズ
    //     paramX  ディスプレースメントまたはインデックスレジスタ
    //  subscript  'w'または'l'
    ETY_SIZE {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlParamX.exlAppendTo (sb).append ('.').append ((char) elem.exlSubscript);
      }
    },

    //スケールファクタ
    //    paramX  インデックスレジスタまたはサイズ付きインデックスレジスタ
    //    paramY  スケールファクタ
    ETY_SCALE_FACTOR {  // x*y
      @Override protected int etyPriority () {
        return EPY_PRIORITY_MULTIPLICATION;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendBinaryOperatorTo (sb, "*");
      }
    },

    //k-factor
    //  <ea>{<k-factor>}
    //  paramX  ea
    //  paramY  k-factor。浮動小数点数またはイミディエイトまたはデータレジスタ
    ETY_K_FACTOR {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlParamY.exlAppendTo (elem.exlParamX.exlAppendTo (sb).append ('{')).append ('}');
      }
    },

    //ビットフィールド
    //  <ea>{<offset>:<width>}
    //  paramX  ea
    //  paramY  offset。浮動小数点数またはイミディエイトまたはデータレジスタ
    //  paramZ  width。浮動小数点数またはイミディエイトまたはデータレジスタ
    ETY_BIT_FIELD {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_POSTFIX;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlParamZ.exlAppendTo (elem.exlParamY.exlAppendTo (elem.exlParamX.exlAppendTo (sb).append ('{')).append (':')).append ('}');
      }
    },

    //レジスタ間接
    //  (Rr)
    //  subscript  レジスタ番号
    ETY_REGISTER_INDIRECT {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int r = elem.exlSubscript;
        sb.append ('(');
        if (r < 8) {
          sb.append ('d').append (r);
        } else if (r < 15) {
          sb.append ('a').append (r - 8);
        } else {
          sb.append ("sp");
        }
        return sb.append (')');
      }
    },

    //アドレスレジスタ間接ポストインクリメント付き
    //  (Ar)+
    //  subscript  レジスタ番号-8
    ETY_POSTINCREMENT {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int r = elem.exlSubscript;
        sb.append ('(');
        if (r < 7) {
          sb.append ('a').append (r);
        } else {
          sb.append ("sp");
        }
        return sb.append (")+");
      }
    },

    //アドレスレジスタ間接プリデクリメント付き
    //  -(Ar)
    //  subscript  レジスタ番号-8
    ETY_PREDECREMENT {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int r = elem.exlSubscript;
        sb.append ("-(");
        if (r < 7) {
          sb.append ('a').append (r);
        } else {
          sb.append ("sp");
        }
        return sb.append (')');
      }
    },

    //イミディエイト
    //  #<data>
    //  paramX  data
    ETY_IMMEDIATE {
      @Override protected int etyPriority () {
        return EPY_PRIORITY_PREFIX;
      }
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlAppendPrefixOperatorTo (sb, "#");
      }
    },

    //データレジスタペア
    //  Dh:Dl
    //  subsubcript  h<<3|l
    ETY_DATA_REGISTER_PAIR {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int subscript = elem.exlSubscript;
        int h = subscript >> 3;
        int l = subscript & 7;
        return sb.append ('d').append (h).append (":d").append (l);
      }
    },

    //レジスタ間接ペア
    //  (Rr):(Rs)
    //  subscript  r<<4|s
    ETY_REGISTER_INDIRECT_PAIR {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int subscript = elem.exlSubscript;
        int r = subscript >> 4;
        int s = subscript & 15;
        sb.append ('(');
        if (r < 8) {
          sb.append ('d').append (r);
        } else if (r < 15) {
          sb.append ('a').append (r - 8);
        } else {
          sb.append ("sp");
        }
        sb.append ("):(");
        if (s < 8) {
          sb.append ('d').append (s);
        } else if (s < 15) {
          sb.append ('a').append (s - 8);
        } else {
          sb.append ("sp");
        }
        return sb.append (')');
      }
    },

    //整数レジスタリスト
    //  D0-D7/A0-A7
    //  subsubcript   bit0  D0
    //                 :     :
    //                bit7  D7
    //                bit8  A0
    //                 :     :
    //               bit15  A7
    //  ビットの並び順を反転しなければならない場合があることに注意
    ETY_INTEGER_REGISTER_LIST {  //D0-D7/A0-A7
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int m = elem.exlSubscript;
        m = (m & 0x8000) << 2 | (m & 0x7f00) << 1 | (m & 0x00ff);  //D7/A0とA6/SPを不連続にする
        boolean s = false;
        while (m != 0) {
          int i = Integer.numberOfTrailingZeros (m);
          m += 1 << i;
          int j = Integer.numberOfTrailingZeros (m);
          m -= 1 << j;
          j--;
          if (s) {
            sb.append ('/');
          }
          if (i <= 7) {
            sb.append ('d').append (i);
          } else if (i <= 16) {
            sb.append ('a').append (i - 9);
          } else {
            sb.append ("sp");
          }
          if (i < j) {
            sb.append ('-');
            if (j <= 7) {
              sb.append ('d').append (j);
            } else if (j <= 16) {
              sb.append ('a').append (j - 9);
            } else {
              sb.append ("sp");
            }
          }
          s = true;
        }  //while m!=0
        return sb;
      }
    },

    //浮動小数点レジスタリスト
    //  FP0-FP7
    //  subsubcript  bit0  FP0
    //                :     :
    //               bit7  FP7
    //  ビットの並び順を反転しなければならない場合があることに注意
    ETY_FLOATING_POINT_REGISTER_LIST {  //FP0-FP7
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int m = elem.exlSubscript;
        boolean s = false;
        while (m != 0) {
          int i = Integer.numberOfTrailingZeros (m);
          m += 1 << i;
          int j = Integer.numberOfTrailingZeros (m);
          m -= 1 << j;
          j--;
          if (s) {
            sb.append ('/');
          }
          sb.append ("fp").append (i);
          if (i < j) {
            sb.append ("-fp").append (j);
          }
          s = true;
        }  //while m!=0
        return sb;
      }
    },

    //浮動小数点制御レジスタリスト
    //  FPIAR/FPSR/FPCR
    //  subsubcript  bit0  FPIAR
    //               bit1  FPSR
    //               bit2  FPCR
    //  浮動小数点制御レジスタは1個だけでも浮動小数点制御レジスタリストになる
    ETY_FLOATING_POINT_CONTROL_REGISTER_LIST {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int subscript = elem.exlSubscript;
        return sb.append (subscript == 1 ? "fpiar" :
                          subscript == 2 ? "fpsr" :
                          subscript == 3 ? "fpiar/fpsr" :
                          subscript == 4 ? "fpcr" :
                          subscript == 5 ? "fpiar/fpcr" :
                          subscript == 6 ? "fpsr/fpcr" :
                          subscript == 7 ? "fpiar/fpsr/fpcr" :
                          "");
      }
    },

    //ラベル定義
    //  <label>:
    //  stringValue  label
    ETY_LABEL_DEFINITION {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue).append (':');
      }
    },

    //ローカルラベル定義
    //  @@:など
    //  subscript  番号
    //    @@:  0
    //     1:  1
    //     2:  2
    ETY_LOCAL_LABEL_DEFINITION {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        if (elem.exlSubscript == 0) {
          sb.append ("@@");
        } else {
          sb.append (elem.exlSubscript);
        }
        return sb.append (':');
      }
    },

    //ローカルラベル参照
    //  @Fなど
    //  subscript  オフセット<<16|番号
    //    @@@B  -3<<16|0
    //    @@B   -2<<16|0
    //    @B    -1<<16|0
    //    @F     0<<16|0
    //    @@F    1<<16|0
    //    @@@F   2<<16|0
    //      @は256個まで
    //    1B    -1<<16|1
    //    1F     0<<16|1
    //    2B    -1<<16|2
    //    2F     0<<16|2
    //      数字は9999まで
    ETY_LOCAL_LABEL_REFERENCE {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        int number = (char) elem.exlSubscript;  //番号
        int offset = elem.exlSubscript >> 16;  //オフセット
        if (number == 0) {
          for (int i = offset < 0 ? ~offset : offset; 0 <= i; i--) {
            sb.append ('@');
          }
        } else {
          sb.append (number);
        }
        return sb.append (offset < 0 ? 'B' : 'F');
      }
    },

    //ニモニック
    //  nopなど
    //  stringValue  nopなど
    ETY_MNEMONIC {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return sb.append (elem.exlStringValue);
      }
    },

    //丸括弧
    //  exlParamX  <x>
    //    (<x>)
    //    省略できない丸括弧
    ETY_PARENTHESIS {
      @Override protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
        return elem.exlParamX.exlAppendTo (sb.append ('(')).append (')');
      }
    },

    //
    ETY_DUMMY;

    protected void etyEval (ExpressionElement elem, int mode) {
    }

    protected StringBuilder etyAppendTo (StringBuilder sb, ExpressionElement elem) {
      sb.append (name ()).append ('(');
      if (elem.exlParamX != null) {
        sb.append (elem.exlParamX.toString ());
        if (elem.exlParamY != null) {
          sb.append (',').append (elem.exlParamY.toString ());
          if (elem.exlParamZ != null) {
            sb.append (',').append (elem.exlParamZ.toString ());
          }
        }
      }
      return sb.append (')');
    }

    protected int etyPriority () {
      return EPY_PRIORITY_PRIMITIVE;
    }

  };  //enum ElementType



  //========================================================================================
  //$$EXL 式の要素
  //  class ExpressionElement
  protected class ExpressionElement {


    //------------------------------------------------------------------------
    //インスタンスフィールド
    protected ElementType exlType;  //要素の種類
    protected int exlSubscript;  //レジスタの番号
    protected ElementType exlValueType;  //結果の型。ETY_FLOATまたはETY_STRING
    protected EFP exlFloatValue;  //ETY_FLOATの値
    protected String exlStringValue;  //ETY_STRINGの値
    protected String exlSource;  //ソース
    protected int exlOffset;  //ソースの該当部分の開始位置
    protected int exlLength;  //ソースの該当部分の長さ
    protected ExpressionElement exlParamX;  //関数と演算子の引数
    protected ExpressionElement exlParamY;
    protected ExpressionElement exlParamZ;


    //------------------------------------------------------------------------
    //コンストラクタ
    protected ExpressionElement (ElementType type, int subscript,
                                 ElementType valueType, EFP floatValue, String stringValue,
                                 String source, int offset, int length) {
      exlType = type;
      exlSubscript = subscript;
      exlValueType = valueType;
      exlFloatValue = floatValue == null ? new EFP () : new EFP (floatValue);
      exlStringValue = stringValue == null ? "" : stringValue;
      exlSource = source;
      exlOffset = offset;
      exlLength = length;
      exlParamX = null;
      exlParamY = null;
      exlParamZ = null;
    }
    protected ExpressionElement (ElementType type, int subscript,
                                 ElementType valueType, EFP floatValue, String stringValue,
                                 String source, int offset, int length,
                                 ExpressionElement paramX, ExpressionElement paramY, ExpressionElement paramZ) {
      exlType = type;
      exlSubscript = subscript;
      exlValueType = valueType;
      exlFloatValue = floatValue == null ? new EFP () : new EFP (floatValue);
      exlStringValue = stringValue == null ? "" : stringValue;
      exlSource = source;
      exlOffset = offset;
      exlLength = length;
      exlParamX = paramX;
      exlParamY = paramY;
      exlParamZ = paramZ;
    }


    //------------------------------------------------------------------------
    //コピー
    protected ExpressionElement exlCopy () {
      ExpressionElement elem = new ExpressionElement (exlType, exlSubscript,
                                                      exlValueType, new EFP (exlFloatValue), exlStringValue,
                                                      exlSource, exlOffset, exlLength);
      if (exlParamX != null) {
        if (exlType == ElementType.ETY_VARIABLE_FLOAT ||
            exlType == ElementType.ETY_VARIABLE_STRING) {
          elem.exlParamX = exlParamX;  //変数の本体はコピーしない
        } else {
          elem.exlParamX = exlParamX.exlCopy ();
        }
      }
      if (exlParamY != null) {
        elem.exlParamY = exlParamY.exlCopy ();
      }
      if (exlParamZ != null) {
        elem.exlParamZ = exlParamZ.exlCopy ();
      }
      return elem;
    }  //exlCopy


    //------------------------------------------------------------------------
    //丸め桁数
    protected void exlSetRoundingPrec (int prec) {
      if (0 <= prec && prec <= 4) {
        epbRoundingPrec = prec;
      }
    }


    //------------------------------------------------------------------------
    //丸めモード
    protected void exlSetRoundingMode (int mode) {
      if (0 <= mode && mode <= 3) {
        epbRoundingMode = mode;
      }
    }


    //------------------------------------------------------------------------
    //整数レジスタ
    protected int exlReadRegByte (int n) {
      if (0 <= n && n <= 15) {
        return (byte) XEiJ.regRn[n];
      }
      return 0;
    }

    protected int exlReadRegWord (int n) {
      if (0 <= n && n <= 15) {
        return (short) XEiJ.regRn[n];
      }
      return 0;
    }

    protected int exlReadRegLong (int n) {
      if (0 <= n && n <= 15) {
        return XEiJ.regRn[n];
      }
      return 0;
    }

    protected void exlWriteRegByte (int n, int x) {
      if (0 <= n && n <= 15) {
        XEiJ.regRn[n] = XEiJ.regRn[n] & ~255 | x & 255;
      }
    }

    protected void exlWriteRegWord (int n, int x) {
      if (0 <= n && n <= 15) {
        XEiJ.regRn[n] = XEiJ.regRn[n] & ~65535 | x & 65535;
      }
    }

    protected void exlWriteRegLong (int n, int x) {
      if (0 <= n && n <= 15) {
        XEiJ.regRn[n] = x;
      }
    }


    //------------------------------------------------------------------------
    //浮動小数点レジスタ
    protected EFP exlGetFPn (int n) {
      return ExpressionEvaluator.this.epbFPn[n];
    }
    protected void exlSetFPn (int n, EFPBox.EFP x) {
      ExpressionEvaluator.this.epbFPn[n].sete (x);
    }


    //------------------------------------------------------------------------
    //制御レジスタ
    protected int exlReadPC () {
      return XEiJ.regPC;
    }

    protected int exlWritePC (int x) {
      return XEiJ.regPC = x;
    }

    protected int exlReadCCR () {
      return XEiJ.regCCR;
    }

    protected int exlWriteCCR (int x) {
      return XEiJ.regCCR = x &= XEiJ.REG_CCR_MASK;
    }

    protected int exlReadSR () {
      return XEiJ.regSRT1 | XEiJ.regSRT0 | XEiJ.regSRS | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR;
    }

    protected int exlWriteSR (int x) {
      XEiJ.regSRT1 = x & XEiJ.REG_SR_T1;
      XEiJ.regSRT0 = x & XEiJ.REG_SR_T0;
      //XEiJ.regSRS = x & XEiJ.REG_SR_S;
      XEiJ.regSRM = x & XEiJ.REG_SR_M;
      XEiJ.regSRI = x & XEiJ.REG_SR_I;
      XEiJ.regCCR = x & XEiJ.REG_CCR_MASK;
      return x &= XEiJ.REG_SR_T1 | XEiJ.REG_SR_T0 | XEiJ.REG_SR_M | XEiJ.REG_SR_I | XEiJ.REG_CCR_MASK;
    }

    protected int exlReadFloatControlRegister (int n) {
      switch (n & 7) {
      case 1:
        return XEiJ.fpuBox.epbFpiar;
      case 2:
        return XEiJ.fpuBox.epbFpsr;
      case 4:
        return XEiJ.fpuBox.epbFpcr;
      }
      return 0;
    }

    protected int exlWriteFloatControlRegister (int n, int x) {
      switch (n & 7) {
      case 1:
        return XEiJ.fpuBox.epbFpiar = x;
      case 2:
        return XEiJ.fpuBox.epbFpsr = x;
      case 4:
        return XEiJ.fpuBox.epbFpcr = x;
      }
      return x;
    }

    protected int exlReadControlRegister (int n) {
      switch ((char) n) {
      case 0x0000:
        return XEiJ.mpuSFC;
      case 0x0001:
        return XEiJ.mpuDFC;
      case 0x0002:
        return XEiJ.mpuCACR;
      case 0x0003:
        return MC68060.mmuTCR;
      case 0x0004:
        return MC68060.mmuITT0;
      case 0x0005:
        return MC68060.mmuITT1;
      case 0x0006:
        return MC68060.mmuDTT0;
      case 0x0007:
        return MC68060.mmuDTT1;
      case 0x0008:
        return XEiJ.mpuBUSCR;
      case 0x0800:
        return XEiJ.regSRS != 0 ? XEiJ.mpuUSP : XEiJ.regRn[15];
      case 0x0801:
        return XEiJ.mpuVBR;
      case 0x0802:
        return XEiJ.mpuCAAR;
      case 0x0803:
        return XEiJ.regSRS == 0 || XEiJ.regSRM == 0 ? XEiJ.mpuMSP : XEiJ.regRn[15];
      case 0x0804:
        return XEiJ.regSRS == 0 || XEiJ.regSRM != 0 ? XEiJ.mpuISP : XEiJ.regRn[15];
      case 0x0805:
        return 0;  //MC68040.mmuMMUSR;
      case 0x0806:
        return MC68060.mmuURP;
      case 0x0807:
        return MC68060.mmuSRP;
      case 0x0808:
        return XEiJ.mpuPCR;
      }
      return 0;
    }  //exlReadControlRegister(int)

    protected int exlWriteControlRegister (int n, int x) {
      switch ((char) n) {
      case 0x0000:
        return XEiJ.mpuSFC = x & 0x00000007;
      case 0x0001:
        return XEiJ.mpuDFC = x & 0x00000007;
      case 0x0002:
        return XEiJ.mpuCACR = x & (XEiJ.currentMPU < Model.MPU_MC68LC040 ? 0x00003f1f : 0xf8e0e000);
      case 0x0003:
        return MC68060.mmuTCR = x;
      case 0x0004:
        return MC68060.mmuITT0 = x;
      case 0x0005:
        return MC68060.mmuITT1 = x;
      case 0x0006:
        return MC68060.mmuDTT0 = x;
      case 0x0007:
        return MC68060.mmuDTT1 = x;
      case 0x0008:
        return XEiJ.mpuBUSCR = x & 0xf0000000;
      case 0x0800:
        return XEiJ.regSRS == 0 ? (XEiJ.regRn[15] = x) : (XEiJ.mpuUSP = x);
      case 0x0801:
        return XEiJ.mpuVBR = x & -4;
      case 0x0802:
        return XEiJ.mpuCAAR = x;
      case 0x0803:
        return XEiJ.regSRS == 0 || XEiJ.regSRM == 0 ? (XEiJ.mpuMSP = x) : (XEiJ.regRn[15] = x);
      case 0x0804:
        return XEiJ.regSRS == 0 || XEiJ.regSRM != 0 ? (XEiJ.mpuISP = x) : (XEiJ.regRn[15] = x);
      case 0x0805:
        return 0;  //MC68040.mmuMMUSR = x;
      case 0x0806:
        return MC68060.mmuURP = x;
      case 0x0807:
        return MC68060.mmuSRP = x;
      case 0x0808:
        return XEiJ.mpuPCR = 0x04300500 | XEiJ.MPU_060_REV << 8 | x & 0x00000083;
      }
      return x;
    }  //exlWriteControlRegister(int,int)


    //--------------------------------------------------------------------------------
    //b = elem.exlIsFloatSubstituend ()
    //  数値被代入項か
    //  代入演算子とインクリメント・デクリメント演算子の被演算項を確認するときに使う
    protected boolean exlIsFloatSubstituend () {
      return (exlType == ElementType.ETY_VARIABLE_FLOAT ||  // 浮動小数点変数
              //exlType == ElementType.ETY_VARIABLE_STRING ||  // 文字列変数
              exlType == ElementType.ETY_INTEGER_REGISTER ||  // d0
              exlType == ElementType.ETY_FLOATING_POINT_REGISTER ||  // fp0
              exlType == ElementType.ETY_PC ||  // pc
              exlType == ElementType.ETY_CCR ||  // ccr
              exlType == ElementType.ETY_SR ||  // sr
              exlType == ElementType.ETY_FLOAT_CONTROL_REGISTER ||  // fpiar
              exlType == ElementType.ETY_CONTROL_REGISTER ||  // sfc
              exlType == ElementType.ETY_SQUARE_BRACKET ||  // [x]
              ((exlType == ElementType.ETY_OPERATOR_SIZE_BYTE ||
                exlType == ElementType.ETY_OPERATOR_SIZE_WORD ||
                exlType == ElementType.ETY_OPERATOR_SIZE_LONG ||
                exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE) &&
               (exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER ||  // d0.b,d0.w,d0.l,d0.s
                exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET)) ||  // [x].b,[x].w,[x].l,[x].s
              ((exlType == ElementType.ETY_OPERATOR_SIZE_QUAD ||
                exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE ||
                exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED ||
                exlType == ElementType.ETY_OPERATOR_SIZE_TRIPLE ||
                exlType == ElementType.ETY_OPERATOR_SIZE_PACKED) &&
               exlParamX.exlType == ElementType.ETY_SQUARE_BRACKET));  // [x].q [x].d [x].x [x].t [x].p
    }  //exlIsFloatSubstituend


    //------------------------------------------------------------------------
    //  式エバリュエータ
    //  式の内部表現を評価する
    protected ExpressionElement exlEval (int mode) {
      exlType.etyEval (this, mode);
      return this;
    }  //exlEval()


    //------------------------------------------------------------------------
    //  コンマリストの長さを数える
    protected int exlLengthOfCommaList () {
      return exlType == ElementType.ETY_OPERATOR_COMMA ? exlParamX.exlLengthOfCommaList () + 1 : 1;
    }


    //------------------------------------------------------------------------
    //  コンマ演算子で連結された式を左から順に評価して結果を配列にして返す
    protected ExpressionElement[] exlEvalCommaList (int mode) {
      return exlEvalCommaListSub (new ArrayList<ExpressionElement> (), mode).toArray (new ExpressionElement[0]);
    }
    protected ArrayList<ExpressionElement> exlEvalCommaListSub (ArrayList<ExpressionElement> list, int mode) {
      if (exlType == ElementType.ETY_OPERATOR_COMMA) {
        exlParamX.exlEvalCommaListSub (list, mode);
        list.add (exlParamY.exlEval (mode));
      } else {
        list.add (exlEval (mode));
      }
      return list;
    }


    //------------------------------------------------------------------------
    //  コンマリストをリストに変換する
    protected LinkedList<ExpressionElement> exlToCommaList () {
      LinkedList<ExpressionElement> list = new LinkedList<ExpressionElement> ();
      ExpressionElement element = this;
      while (element.exlType == ElementType.ETY_OPERATOR_COMMA) {
        list.addFirst (element.exlParamY);
        element = element.exlParamX;
      }
      list.addFirst (element);
      return list;
    }  //exlToCommaList


    //------------------------------------------------------------------------
    //  セパレータリストをリストに変換する
    protected LinkedList<ExpressionElement> exlToSeparatorList () {
      LinkedList<ExpressionElement> list = new LinkedList<ExpressionElement> ();
      ExpressionElement element = this;
      while (element.exlType == ElementType.ETY_SEPARATOR) {
        list.addFirst (element.exlParamY);
        element = element.exlParamX;
      }
      list.addFirst (element);
      return list;
    }  //exlToSeparatorList


    //------------------------------------------------------------------------
    protected void exlPrint () {
      switch (exlValueType) {
      case ETY_FLOAT:
        DebugConsole.dgtPrint (exlFloatValue.toString ());
        break;
      case ETY_STRING:
        DebugConsole.dgtPrint (exlStringValue);
        break;
      }
    }


    //------------------------------------------------------------------------
    //  式の内部表現を文字列に変換する
    protected StringBuilder exlAppendTo (StringBuilder sb) {
      return exlType.etyAppendTo (sb, this);
    }


    //------------------------------------------------------------------------
    //  関数呼び出し
    protected StringBuilder exlAppendFunctionTo (StringBuilder sb, String funcName) {
      sb.append (funcName).append ('(');
      if (exlParamX != null) {
        exlParamX.exlAppendTo (sb);
        if (exlParamY != null) {
          exlParamY.exlAppendTo (sb.append (','));
          if (exlParamZ != null) {
            exlParamZ.exlAppendTo (sb.append (','));
          }
        }
      }
      return sb.append (')');
    }


    //------------------------------------------------------------------------
    //  後置演算子
    //    左辺の優先順位が自分と同じか自分より低いとき左辺を括弧で囲む
    protected StringBuilder exlAppendPostfixOperatorTo (StringBuilder sb, String text) {
      if (exlParamX.exlType.etyPriority () <= exlType.etyPriority ()) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      return sb.append (text);
    }


    //------------------------------------------------------------------------
    //  前置演算子
    //    右辺の優先順位が自分と同じか自分より低いとき右辺を括弧で囲む
    protected StringBuilder exlAppendPrefixOperatorTo (StringBuilder sb, String text) {
      sb.append (text);
      if (exlParamX.exlType.etyPriority () <= exlType.etyPriority ()) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      return sb;
    }


    //------------------------------------------------------------------------
    //  二項演算子
    //    左から結合する
    //    左辺の優先順位が自分より低いとき左辺を括弧で囲む
    //    右辺の優先順位が自分と同じか自分より低いとき右辺を括弧で囲む
    protected StringBuilder exlAppendBinaryOperatorTo (StringBuilder sb, String text) {
      if (exlParamX.exlType.etyPriority () < exlType.etyPriority ()) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      sb.append (text);
      if (exlParamY.exlType.etyPriority () <= exlType.etyPriority ()) {
        exlParamY.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamY.exlAppendTo (sb);
      }
      return sb;
    }


    //------------------------------------------------------------------------
    //  条件演算子
    //    右から結合する
    //    左辺の優先順位が自分と同じか自分より低いとき左辺を括弧で囲む
    //    中辺と右辺は括弧で囲まない
    protected StringBuilder exlAppendConditionalOperatorTo (StringBuilder sb, String text1, String text2) {
      if (exlParamX.exlType.etyPriority () <= exlType.etyPriority ()) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      return exlParamZ.exlAppendTo (exlParamY.exlAppendTo (sb.append (text1)).append (text2));
    }


    //------------------------------------------------------------------------
    //  代入演算子
    //    右から結合する
    //    左辺の優先順位が自分と同じか自分より低いとき左辺を括弧で囲む
    //    右辺の優先順位が自分より低いとき右辺を括弧で囲む
    protected StringBuilder exlAppendAssignmentOperatorTo (StringBuilder sb, String text) {
      if (exlParamX.exlType.etyPriority () <= exlType.etyPriority ()) {
        exlParamX.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamX.exlAppendTo (sb);
      }
      sb.append (text);
      if (exlParamY.exlType.etyPriority () < exlType.etyPriority ()) {
        exlParamY.exlAppendTo (sb.append ('(')).append (')');
      } else {
        exlParamY.exlAppendTo (sb);
      }
      return sb;
    }


    //------------------------------------------------------------------------
    //  式の内部表現を文字列に変換する
    @Override public String toString () {
      return exlAppendTo (new StringBuilder ()).toString ();
    }


  }  //class ExpressionElement



  //--------------------------------------------------------------------------------
  //  パーサのエラー表示
  protected static void evxPrintError (String message, String source, int offset, int length) {
    StringBuilder sb = new StringBuilder ();
    sb.append (message).append ('\n');
    if (source != null) {
      if (offset == -1) {
        offset = source.length ();
      }
      int head = Math.max (0, offset - 20);
      int tail = Math.min (source.length (), offset + length + 20);
      sb.append (source.substring (head, tail)).append ('\n');
      for (int i = head; i < offset; i++) {
        sb.append (' ');
      }
      for (int i = 0; i < length; i++) {
        sb.append ('^');
      }
      DebugConsole.dgtPrintln (sb.toString ());
    }
  }


  //--------------------------------------------------------------------------------
  //nodeTree = evxParse (source, mode)
  //  パーサ
  protected ExpressionElement evxParse (String source, int mode) {

    //----------------------------------------------------------------
    //語彙解析
    //  文字列をトークンリストに変換する
    LinkedList<ExpressionElement> tokenList = new LinkedList<ExpressionElement> ();
    ExpressionElement lastToken = null;  //直前のトークン。null=行頭またはセパレータの直後
    char[] a = source.toCharArray ();  //文字の配列
    int field = 0;
    int l = a.length;  //文字の配列の長さ
    int p = 0;  //次の文字の位置
    int c = p < l ? a[p++] : -1;
    while (0 <= c) {
      //          0x20        0x08         0x09         0x0a         0x0c         0x0d
      while (c == ' ' || c == '\b' || c == '\t' || c == '\n' || c == '\f' || c == '\r') {  //空白
        if (c == '\n' || c == '\r') {
          //改行をセパレータとみなす
          if (!(lastToken != null && lastToken.exlType == ElementType.ETY_TOKEN_SEMICOLON)) {
            tokenList.add (new ExpressionElement (ElementType.ETY_TOKEN_SEMICOLON, 0,
                                                  ElementType.ETY_UNDEF, null, "",
                                                  source, p - 1, 1));
          }
          lastToken = null;  //行頭
        }
        c = p < l ? a[p++] : -1;
      }
      if (c < 0) {
        break;
      }
      int p0 = p - 1;  //1文字目の位置
      ElementType type = ElementType.ETY_FLOAT;
      int subscript = 0;
      ElementType valueType = ElementType.ETY_UNDEF;
      EFP floatValue = null;
      String stringValue = "";
    token:
      {

        if (mode == EVM_ASSEMBLER) {  //アセンブラモード
          int q = p;
          int d = c;
          if (lastToken == null) {  //行頭またはセパレータの直後
            //ローカルラベル定義
            int number = -1;
            if (d == '@') {
              d = q < l ? a[q++] : -1;
              if (d == '@') {
                d = q < l ? a[q++] : -1;
              }
            } else if ('1' <= d && d <= '9') {
              number = d - '0';
              d = q < l ? a[q++] : -1;
              for (int i = 2; i <= 4 && '0' <= d && d <= '9'; i++) {  //2..4桁目
                number = number * 10 + (d - '0');
                d = q < l ? a[q++] : -1;
              }
            }
            if (d == '$' || d == '@' ||
                '0' <= d && d <= '9' || 'A' <= d && d <= 'Z' || 'a' <= d && d <= 'z') {
              number = -1;
            }
            if (0 <= number) {
              p = q;
              c = d;
              while (c == ':') {  //直後の':'の並びを読み飛ばす
                c = p < l ? a[p++] : -1;
              }
              type = ElementType.ETY_LOCAL_LABEL_DEFINITION;
              subscript = number;
              break token;
            }
          } else {  //行頭またはセパレータの直後ではない
            //ローカルラベル参照
            int number = -1;
            int offset = 0;
            if (d == '@') {
              number = 0;
              d = q < l ? a[q++] : -1;
              for (int i = 2; i <= 256 && d == '@'; i++) {  //2..256文字目
                offset++;
                d = q < l ? a[q++] : -1;
              }
            } else if ('1' <= d && d <= '9') {
              number = d - '0';
              d = q < l ? a[q++] : -1;
              for (int i = 2; i <= 4 && '0' <= d && d <= '9'; i++) {  //2..4桁目
                number = number * 10 + (d - '0');
                d = q < l ? a[q++] : -1;
              }
            }
            if (0 <= number) {
              if (d == 'B' || d == 'b') {  //後方参照
                d = q < l ? a[q++] : -1;
                offset = ~offset;
              } else if (d == 'F' || d == 'f') {  //前方参照
                d = q < l ? a[q++] : -1;
              } else {
                number = -1;
              }
            }
            if (0 <= number) {
              if (d == '$' || d == '@' ||
                  '0' <= d && d <= '9' || 'A' <= d && d <= 'Z' || 'a' <= d && d <= 'z') {
                number = -1;
              }
            }
            if (0 <= number) {
              p = q;
              c = d;
              type = ElementType.ETY_LOCAL_LABEL_REFERENCE;
              subscript = offset << 16 | number;
              break token;
            }
          }  //if 行頭またはセパレータの直後/ではない
        }  //if アセンブラモード

        //----------------------------------------
        //浮動小数点数
      number:
        {
          int d = p < l ? a[p] : -1;
          int radix;
          int check = 1;  //1=整数部に数字がない,2=小数部に数字がない,4=指数部に数字がない,8=不明な接尾辞がある。10進数のときは既に整数部に数字がある
          if (c == '0') {
            if (d == 'X' || d == 'x') {  //16進数
              p++;
              radix = 16;
            } else if (d == 'O' || d == 'o') {  //8進数
              p++;
              radix = 8;
            } else if (d == 'B' || d == 'b') {  //2進数
              p++;
              radix = 2;
            } else {  //10進数
              radix = 10;
              check = 0;  //整数部に数字がある
            }
          } else if ('1' <= c && c <= '9') {  //10進数
            radix = 10;
            check = 0;  //整数部に数字がある
          } else if (c == '$' &&
                     (('0' <= d && d <= '9') || ('A' <= d && d <= 'F') || ('a' <= d && d <= 'f') || d == '_')) {  //16進数
            radix = 16;
          } else if (c == '@' &&
                     (('0' <= d && d <= '7') || d == '_')) {  //8進数
            radix = 8;
          } else if (c == '%' &&
                     (('0' <= d && d <= '1') || d == '_')) {  //2進数
            radix = 2;
          } else {
            break number;
          }
          //整数部
          c = p < l ? a[p++] : -1;  //10進数は2桁目、それ以外は1桁目
          while ((radix <= 10 ?
                  '0' <= c && c < '0' + radix :  //2進数,8進数,10進数
                  '0' <= c && c <= '9' || 'A' <= c && c <= 'F' || 'a' <= c && c <= 'f') ||  //16進数
                 c == '_') {
            if (c != '_') {
              check &= ~1;  //整数部に数字がある
            }
            c = p < l ? a[p++] : -1;
          }
          //小数部
          if (c == '.') {  //小数点?
            d = p < l ? a[p] : -1;  //'.'の直後
            if ((radix <= 10 ?
                 '0' <= d && d < '0' + radix :  //2進数,8進数,10進数
                 '0' <= d && d <= '9' || 'A' <= d && d <= 'F' || 'a' <= d && d <= 'f') ||  //16進数
                d == '_' ||
                (radix == 10 ? d == 'E' || d == 'e' : d == 'P' || d == 'p')) {  //小数部または指数部がある
              check |= 2;  //小数部があるが小数部に数字がない
              c = p < l ? a[p++] : -1;
              while ((radix <= 10 ?
                      '0' <= c && c < '0' + radix :  //2進数,8進数,10進数
                      '0' <= c && c <= '9' || 'A' <= c && c <= 'F' || 'a' <= c && c <= 'f') ||  //16進数
                     c == '_') {
                if (c != '_') {
                  check &= ~2;  //小数部に数字がある
                }
                c = p < l ? a[p++] : -1;
              }
            }
          }
          //指数部
          if (radix == 10 ?
              c == 'E' || c == 'e' :  //10進数
              c == 'P' || c == 'p') {  //2進数,8進数,16進数。指数部がある
            check |= 4;  //指数部があるが指数部に数字がない
            c = p < l ? a[p++] : -1;
            if (c == '+' || c == '-') {
              c = p < l ? a[p++] : -1;
            }
            while ('0' <= c && c <= '9') {
              check &= ~4;  //指数部に数字がある
              c = p < l ? a[p++] : -1;
            }
          }
          //接尾辞
          while ('A' <= c && c <= 'Z' || 'a' <= c & c <= 'z') {  //接尾辞がある
            check |= 8;  //不明な接尾辞がある
            c = p < l ? a[p++] : -1;
          }
          if (check != 0) {
            evxPrintError ((check & 1) != 0 ? (Multilingual.mlnJapanese ?
                                               "整数部に数字がありません" :
                                               "no figure appears at the integer part") :
                           (check & 2) != 0 ? (Multilingual.mlnJapanese ?
                                               "小数部に数字がありません" :
                                               "no figure appears at the fractional part") :
                           (check & 4) != 0 ? (Multilingual.mlnJapanese ?
                                               "指数部に数字がありません" :
                                               "no figure appears at the exponential part") :
                           (check & 8) != 0 ? (Multilingual.mlnJapanese ?
                                               "浮動小数点数に不明な接尾辞が続いています" :
                                               "floating point number followed by an unknown postfix") :
                           "",
                           source, p0, p - p0);
            return null;
          }
          type = ElementType.ETY_FLOAT;
          floatValue = new EFP (String.valueOf (a, p0, p - p0));
          break token;
        }  //number 浮動小数点数

        //----------------------------------------
        //文字
        if (c == '\'') {  //文字
          c = p < l ? a[p++] : -1;
          if (c < 0) {
            evxPrintError (Multilingual.mlnJapanese ?
                           "'...' が閉じていません" :
                           "'...' is not closed",
                           source, p0, p - p0);
            return null;
          }
          type = ElementType.ETY_FLOAT;
          floatValue = new EFP ((char) c);
          c = p < l ? a[p++] : -1;
          if (c != '\'') {
            evxPrintError (Multilingual.mlnJapanese ?
                           "'...' が閉じていません" :
                           "'...' is not closed",
                           source, p0, p - p0);
            return null;
          }
          c = p < l ? a[p++] : -1;
          break token;
        }  //文字

        //----------------------------------------
        //文字列
        if (c == '"') {  //文字列
          StringBuilder sb = new StringBuilder ();
          c = p < l ? a[p++] : -1;
          while (0 <= c && c != '"' && c != '\n') {
            if (c == '\\') {  //エスケープ文字
              c = p < l ? a[p++] : -1;
              if (c == '\n') {  //改行を跨ぐ
                c = p < l ? a[p++] : -1;
                continue;
              }
              if ('0' <= c && c <= '3') {  //8進数1～3桁
                c -= '0';
                int d = p < l ? a[p] : -1;
                if ('0' <= d && d <= '7') {
                  p++;
                  c = (c << 3) + (d - '0');
                  d = p < l ? a[p] : -1;
                  if ('0' <= d && d <= '7') {
                    p++;
                    c = (c << 3) + (d - '0');
                  }
                }
              } else if ('4' <= c && c <= '7') {  //8進数1～2桁
                c -= '0';
                int d = p < l ? a[p] : -1;
                if ('0' <= d && d <= '7') {
                  p++;
                  c = (c << 3) + (d - '0');
                }
              } else if (c == 'b') {
                c = '\b';
              } else if (c == 'f') {
                c = '\f';
              } else if (c == 'n') {
                c = '\n';
              } else if (c == 'r') {
                c = '\r';
              } else if (c == 't') {
                c = '\t';
              } else if (c == 'x') {  //16進数2桁
                c = 0;
                for (int i = 0; i < 2; i++) {
                  int d = p < l ? a[p++] : -1;
                  if ('0' <= d && d <= '9' || 'A' <= d && d <= 'F' || 'a' <= d && d <= 'f') {
                    evxPrintError (Multilingual.mlnJapanese ?
                                   "\\x?? が途切れています" :
                                   "unfinished \\x??",
                                   source, p - i - 3, i + 2);
                    return null;
                  }
                  c = (c << 4) + (d <= '9' ? d - '0' : (d | 0x20) - ('a' - 10));
                }
              } else if (c == 'u') {  //16進数4桁
                c = 0;
                for (int i = 0; i < 4; i++) {
                  int d = p < l ? a[p++] : -1;
                  if ('0' <= d && d <= '9' || 'A' <= d && d <= 'F' || 'a' <= d && d <= 'f') {
                    evxPrintError (Multilingual.mlnJapanese ?
                                   "\\u???? が途切れています" :
                                   "unfinished \\u????",
                                   source, p - i - 3, i + 2);
                    return null;
                  }
                  c = (c << 4) + (d <= '9' ? d - '0' : (d | 0x20) - ('a' - 10));
                }
              } else if (c == '\"') {
              } else if (c == '\'') {
              } else if (c == '\\') {
              } else {
                evxPrintError (Multilingual.mlnJapanese ?
                               "不明なエスケープシーケンスです" :
                               "unknown escape sequence",
                               source, p - 3, 2);
                return null;
              }
            }  //エスケープ文字
            sb.append ((char) c);
            c = p < l ? a[p++] : -1;
          }
          if (c != '"') {
            evxPrintError (Multilingual.mlnJapanese ?
                           "\"...\" が閉じていません" :
                           "\"...\" is not closed",
                           source, p0, p - p0);
            return null;
          }
          c = p < l ? a[p++] : -1;
          type = ElementType.ETY_STRING;
          stringValue = sb.toString ();
          break token;
        }  //文字列

        //----------------------------------------
        //識別子
        if ('A' <= c && c <= 'Z' || 'a' <= c & c <= 'z' || c == '_') {  //識別子
          c = p < l ? a[p++] : -1;
          while ('A' <= c && c <= 'Z' || 'a' <= c & c <= 'z' || c == '_' || '0' <= c && c <= '9' || c == '$') {
            c = p < l ? a[p++] : -1;
          }
          String identifier = String.valueOf (a, p0, (c < 0 ? p : p - 1) - p0);
          String lowerIdentifier = identifier.toLowerCase ();
          stringValue = lowerIdentifier;
          if (mode == EVM_COMMAND) {  //コマンドモード
            if (lastToken == null) {  //行頭またはセパレータの直後
              //コマンド
              switch (lowerIdentifier) {
              case "d":
                type = ElementType.ETY_COMMAND_DUMP;
                subscript = 'b';
                break token;
              case "db":
              case "dw":
              case "dl":
              case "dq":
              case "ds":
              case "dd":
              case "dx":
              case "dt":
              case "dp":
                type = ElementType.ETY_COMMAND_DUMP;
                subscript = lowerIdentifier.charAt (1);
                break token;
              case "f":
                type = ElementType.ETY_COMMAND_FILL;
                subscript = 'b';
                break token;
              case "fb":
              case "fw":
              case "fl":
              case "fq":
              case "fs":
              case "fd":
              case "fx":
              case "ft":
              case "fp":
                type = ElementType.ETY_COMMAND_FILL;
                subscript = lowerIdentifier.charAt (1);
                break token;
              case "g":
                type = ElementType.ETY_COMMAND_RUN;
                break token;
              case "h":
                type = ElementType.ETY_COMMAND_HELP;
                break token;
              case "i":
                type = ElementType.ETY_COMMAND_STOP;
                break token;
              case "l":
                type = ElementType.ETY_COMMAND_LIST;
                break token;
              case "ll":
                type = ElementType.ETY_COMMAND_LABEL_LIST;
                break token;
              case "me":
                type = ElementType.ETY_COMMAND_MEMORY_EDIT;
                subscript = 'b';
                break token;
              case "meb":
              case "mew":
              case "mel":
              case "meq":
              case "mes":
              case "med":
              case "mex":
              case "met":
              case "mep":
                type = ElementType.ETY_COMMAND_MEMORY_EDIT;
                subscript = lowerIdentifier.charAt (2);
                break token;
              case "ms":
                type = ElementType.ETY_COMMAND_MEMORY_SEARCH;
                subscript = 'b';
                break token;
              case "msb":
              case "msw":
              case "msl":
              case "msq":
              case "mss":
              case "msd":
              case "msx":
              case "mst":
              case "msp":
                type = ElementType.ETY_COMMAND_MEMORY_SEARCH;
                subscript = lowerIdentifier.charAt (2);
                break token;
              case "p":
                type = ElementType.ETY_COMMAND_PRINT;
                break token;
              case "r":
                type = ElementType.ETY_COMMAND_RETURN;
                break token;
              case "s":
                type = ElementType.ETY_COMMAND_STEP;
                break token;
              case "t":
                type = ElementType.ETY_COMMAND_TRACE;
                break token;
              case "tx":
                type = ElementType.ETY_COMMAND_TRACE_REGS;
                break token;
              case "txf":
                type = ElementType.ETY_COMMAND_TRACE_FLOAT_REGS;
                break token;
              case "x":
                type = ElementType.ETY_COMMAND_REGS;
                break token;
              case "xf":
                type = ElementType.ETY_COMMAND_FLOAT_REGS;
                break token;
              }
            }  //if 行頭またはセパレータの直後
          }  //if コマンドモード
          if (mode == EVM_ASSEMBLER) {  //アセンブラモード
            if (lastToken == null ||  //行頭またはセパレータの直後または
                lastToken.exlType == ElementType.ETY_LABEL_DEFINITION ||  //ラベル定義の直後または
                lastToken.exlType == ElementType.ETY_LOCAL_LABEL_DEFINITION) {  //ローカルラベル定義の直後
              //ニモニック
              if (Assembler.ASM_MNEMONIC_MAP.containsKey (lowerIdentifier)) {
                type = ElementType.ETY_MNEMONIC;
                break token;
              }
            }  //if 行頭またはセパレータの直後またはラベル定義の直後またはローカルラベル定義の直後
            switch (lowerIdentifier) {
              //サプレスされたレジスタ
            case "zd0":
            case "zd1":
            case "zd2":
            case "zd3":
            case "zd4":
            case "zd5":
            case "zd6":
            case "zd7":
              type = ElementType.ETY_ZERO_REGISTER;
              subscript = Integer.parseInt (lowerIdentifier.substring (2));
              break token;
            case "za0":
            case "za1":
            case "za2":
            case "za3":
            case "za4":
            case "za5":
            case "za6":
            case "za7":
              type = ElementType.ETY_ZERO_REGISTER;
              subscript = 8 + Integer.parseInt (lowerIdentifier.substring (2));
              break token;
            case "zsp":
              type = ElementType.ETY_ZERO_REGISTER;
              subscript = 15;
              stringValue = "za7";
              break token;
            case "zr0":
            case "zr1":
            case "zr2":
            case "zr3":
            case "zr4":
            case "zr5":
            case "zr6":
            case "zr7":
            case "zr8":
            case "zr9":
            case "zr10":
            case "zr11":
            case "zr12":
            case "zr13":
            case "zr14":
            case "zr15":
              type = ElementType.ETY_ZERO_REGISTER;
              subscript = Integer.parseInt (lowerIdentifier.substring (2));
              stringValue = subscript < 8 ? "zd" + subscript : "za" + (subscript - 8);
              break token;
            case "zpc":
              type = ElementType.ETY_ZERO_PC;
              break token;
              //オプショナルプログラムカウンタ
            case "opc":
              type = ElementType.ETY_OPTIONAL_PC;
              break token;
              //キャッシュ選択
            case "nc":
              type = ElementType.ETY_CACHE_SELECTION;
              subscript = 0;
              break token;
            case "dc":
              type = ElementType.ETY_CACHE_SELECTION;
              subscript = 1;
              break token;
            case "ic":
              type = ElementType.ETY_CACHE_SELECTION;
              subscript = 2;
              break token;
            case "bc":
              type = ElementType.ETY_CACHE_SELECTION;
              subscript = 3;
              break token;
            }
          }  //if アセンブラモード
          //制御レジスタ
          if (EVX_CONTROL_NAME_TO_MPU_CODE.containsKey (lowerIdentifier)) {
            type = ElementType.ETY_CONTROL_REGISTER;
            subscript = EVX_CONTROL_NAME_TO_MPU_CODE.get (lowerIdentifier);
            break token;
          }
          switch (lowerIdentifier) {
            //制御レジスタ
          case "pc":
            type = ElementType.ETY_PC;
            break token;
          case "ccr":
            type = ElementType.ETY_CCR;
            break token;
          case "sr":
            type = ElementType.ETY_SR;
            break token;
            //浮動小数点制御レジスタ
          case "fpiar":
            type = ElementType.ETY_FLOAT_CONTROL_REGISTER;
            subscript = 1;
            break token;
          case "fpsr":
            type = ElementType.ETY_FLOAT_CONTROL_REGISTER;
            subscript = 2;
            break token;
          case "fpcr":
            type = ElementType.ETY_FLOAT_CONTROL_REGISTER;
            subscript = 4;
            break token;
            //浮動小数点数
          case "infinity":
            type = ElementType.ETY_FLOAT;
            floatValue = INF;
            break token;
          case "nan":
            type = ElementType.ETY_FLOAT;
            floatValue = NAN;
            break token;
            //数学定数
          case "apery":
            type = ElementType.ETY_MATH_APERY;
            break token;
          case "catalan":
            type = ElementType.ETY_MATH_CATALAN;
            break token;
          case "e":
            type = ElementType.ETY_MATH_NAPIER;
            break token;
          case "euler":
            type = ElementType.ETY_MATH_EULER;
            break token;
          case "pi":
            type = ElementType.ETY_MATH_PI;
            break token;
            //レジスタ
          case "d0":
          case "d1":
          case "d2":
          case "d3":
          case "d4":
          case "d5":
          case "d6":
          case "d7":
            type = ElementType.ETY_INTEGER_REGISTER;
            subscript = Integer.parseInt (lowerIdentifier.substring (1));
            break token;
          case "a0":
          case "a1":
          case "a2":
          case "a3":
          case "a4":
          case "a5":
          case "a6":
          case "a7":
            type = ElementType.ETY_INTEGER_REGISTER;
            subscript = 8 + Integer.parseInt (lowerIdentifier.substring (1));
            break token;
          case "sp":
            type = ElementType.ETY_INTEGER_REGISTER;
            subscript = 15;
            stringValue = "a7";
            break token;
          case "r0":
          case "r1":
          case "r2":
          case "r3":
          case "r4":
          case "r5":
          case "r6":
          case "r7":
          case "r8":
          case "r9":
          case "r10":
          case "r11":
          case "r12":
          case "r13":
          case "r14":
          case "r15":
            type = ElementType.ETY_INTEGER_REGISTER;
            subscript = Integer.parseInt (lowerIdentifier.substring (1));
            stringValue = subscript < 8 ? "d" + subscript : "a" + (subscript - 8);
            break token;
          case "fp0":
          case "fp1":
          case "fp2":
          case "fp3":
          case "fp4":
          case "fp5":
          case "fp6":
          case "fp7":
            type = ElementType.ETY_FLOATING_POINT_REGISTER;
            subscript = Integer.parseInt (lowerIdentifier.substring (2));
            break token;
            //関数
          case "abs":
            type = ElementType.ETY_FUNCTION_ABS;
            break token;
          case "acos":
            type = ElementType.ETY_FUNCTION_ACOS;
            break token;
          case "acosh":
            type = ElementType.ETY_FUNCTION_ACOSH;
            break token;
          case "acot":
            type = ElementType.ETY_FUNCTION_ACOT;
            break token;
          case "acoth":
            type = ElementType.ETY_FUNCTION_ACOTH;
            break token;
          case "acsc":
            type = ElementType.ETY_FUNCTION_ACSC;
            break token;
          case "acsch":
            type = ElementType.ETY_FUNCTION_ACSCH;
            break token;
          case "agi":
            type = ElementType.ETY_FUNCTION_AGI;
            break token;
          case "agm":
            type = ElementType.ETY_FUNCTION_AGM;
            break token;
          case "asc":
            type = ElementType.ETY_FUNCTION_ASC;
            break token;
          case "asec":
            type = ElementType.ETY_FUNCTION_ASEC;
            break token;
          case "asech":
            type = ElementType.ETY_FUNCTION_ASECH;
            break token;
          case "asin":
            type = ElementType.ETY_FUNCTION_ASIN;
            break token;
          case "asinh":
            type = ElementType.ETY_FUNCTION_ASINH;
            break token;
          case "atan":
            type = ElementType.ETY_FUNCTION_ATAN;
            break token;
          case "atan2":
            type = ElementType.ETY_FUNCTION_ATAN2;
            break token;
          case "atanh":
            type = ElementType.ETY_FUNCTION_ATANH;
            break token;
          case "bin$":
            type = ElementType.ETY_FUNCTION_BIN_DOLLAR;
            break token;
          case "cbrt":
            type = ElementType.ETY_FUNCTION_CBRT;
            break token;
          case "ceil":
            type = ElementType.ETY_FUNCTION_CEIL;
            break token;
          case "chr$":
            type = ElementType.ETY_FUNCTION_CHR_DOLLAR;
            break token;
          case "cmp":
            type = ElementType.ETY_FUNCTION_CMP;
            break token;
          case "cmp0":
            type = ElementType.ETY_FUNCTION_CMP0;
            break token;
          case "cmp1":
            type = ElementType.ETY_FUNCTION_CMP1;
            break token;
          case "cmp1abs":
            type = ElementType.ETY_FUNCTION_CMP1ABS;
            break token;
          case "cmpabs":
            type = ElementType.ETY_FUNCTION_CMPABS;
            break token;
          case "cos":
            type = ElementType.ETY_FUNCTION_COS;
            break token;
          case "cosh":
            type = ElementType.ETY_FUNCTION_COSH;
            break token;
          case "cot":
            type = ElementType.ETY_FUNCTION_COT;
            break token;
          case "coth":
            type = ElementType.ETY_FUNCTION_COTH;
            break token;
          case "csc":
            type = ElementType.ETY_FUNCTION_CSC;
            break token;
          case "csch":
            type = ElementType.ETY_FUNCTION_CSCH;
            break token;
          case "cub":
            type = ElementType.ETY_FUNCTION_CUB;
            break token;
          case "dec":
            type = ElementType.ETY_FUNCTION_DEC;
            break token;
          case "deg":
            type = ElementType.ETY_FUNCTION_DEG;
            break token;
          case "div2":
            type = ElementType.ETY_FUNCTION_DIV2;
            break token;
          case "div3":
            type = ElementType.ETY_FUNCTION_DIV3;
            break token;
          case "divpi":
            type = ElementType.ETY_FUNCTION_DIVPI;
            break token;
          case "divrz":
            type = ElementType.ETY_FUNCTION_DIVRZ;
            break token;
          case "exp":
            type = ElementType.ETY_FUNCTION_EXP;
            break token;
          case "exp10":
            type = ElementType.ETY_FUNCTION_EXP10;
            break token;
          case "exp2":
            type = ElementType.ETY_FUNCTION_EXP2;
            break token;
          case "exp2m1":
            type = ElementType.ETY_FUNCTION_EXP2M1;
            break token;
          case "expm1":
            type = ElementType.ETY_FUNCTION_EXPM1;
            break token;
          case "floor":
            type = ElementType.ETY_FUNCTION_FLOOR;
            break token;
          case "frac":
            type = ElementType.ETY_FUNCTION_FRAC;
            break token;
          case "getexp":
            type = ElementType.ETY_FUNCTION_GETEXP;
            break token;
          case "getman":
            type = ElementType.ETY_FUNCTION_GETMAN;
            break token;
          case "hex$":
            type = ElementType.ETY_FUNCTION_HEX_DOLLAR;
            break token;
          case "ieeerem":
            type = ElementType.ETY_FUNCTION_IEEEREM;
            break token;
          case "inc":
            type = ElementType.ETY_FUNCTION_INC;
            break token;
          case "iseven":
            type = ElementType.ETY_FUNCTION_ISEVEN;
            break token;
          case "isinf":
            type = ElementType.ETY_FUNCTION_ISINF;
            break token;
          case "isint":
            type = ElementType.ETY_FUNCTION_ISINT;
            break token;
          case "isnan":
            type = ElementType.ETY_FUNCTION_ISNAN;
            break token;
          case "isodd":
            type = ElementType.ETY_FUNCTION_ISODD;
            break token;
          case "isone":
            type = ElementType.ETY_FUNCTION_ISONE;
            break token;
          case "iszero":
            type = ElementType.ETY_FUNCTION_ISZERO;
            break token;
          case "lgamma":
            type = ElementType.ETY_FUNCTION_LGAMMA;
            break token;
          case "log":
            type = ElementType.ETY_FUNCTION_LOG;
            break token;
          case "log10":
            type = ElementType.ETY_FUNCTION_LOG10;
            break token;
          case "log1p":
            type = ElementType.ETY_FUNCTION_LOG1P;
            break token;
          case "log2":
            type = ElementType.ETY_FUNCTION_LOG2;
            break token;
          case "max":
            type = ElementType.ETY_FUNCTION_MAX;
            break token;
          case "min":
            type = ElementType.ETY_FUNCTION_MIN;
            break token;
          case "mul2":
            type = ElementType.ETY_FUNCTION_MUL2;
            break token;
          case "mul3":
            type = ElementType.ETY_FUNCTION_MUL3;
            break token;
          case "mulpi":
            type = ElementType.ETY_FUNCTION_MULPI;
            break token;
          case "oct$":
            type = ElementType.ETY_FUNCTION_OCT_DOLLAR;
            break token;
          case "pow":
            type = ElementType.ETY_FUNCTION_POW;
            break token;
          case "quo":
            type = ElementType.ETY_FUNCTION_QUO;
            break token;
          case "rad":
            type = ElementType.ETY_FUNCTION_RAD;
            break token;
          case "random":
            type = ElementType.ETY_FUNCTION_RANDOM;
            break token;
          case "rcp":
            type = ElementType.ETY_FUNCTION_RCP;
            break token;
          case "rint":
            type = ElementType.ETY_FUNCTION_RINT;
            break token;
          case "rmode":
            type = ElementType.ETY_FUNCTION_RMODE;
            break token;
          case "round":
            type = ElementType.ETY_FUNCTION_ROUND;
            break token;
          case "rprec":
            type = ElementType.ETY_FUNCTION_RPREC;
            break token;
          case "sec":
            type = ElementType.ETY_FUNCTION_SEC;
            break token;
          case "sech":
            type = ElementType.ETY_FUNCTION_SECH;
            break token;
          case "sgn":
            type = ElementType.ETY_FUNCTION_SGN;
            break token;
          case "sin":
            type = ElementType.ETY_FUNCTION_SIN;
            break token;
          case "sinh":
            type = ElementType.ETY_FUNCTION_SINH;
            break token;
          case "sqrt":
            type = ElementType.ETY_FUNCTION_SQRT;
            break token;
          case "squ":
            type = ElementType.ETY_FUNCTION_SQU;
            break token;
          case "str$":
            type = ElementType.ETY_FUNCTION_STR_DOLLAR;
            break token;
          case "tan":
            type = ElementType.ETY_FUNCTION_TAN;
            break token;
          case "tanh":
            type = ElementType.ETY_FUNCTION_TANH;
            break token;
          case "tgamma":
            type = ElementType.ETY_FUNCTION_TGAMMA;
            break token;
          case "trunc":
            type = ElementType.ETY_FUNCTION_TRUNC;
            break token;
          case "ulp":
            type = ElementType.ETY_FUNCTION_ULP;
            break token;
          case "val":
            type = ElementType.ETY_FUNCTION_VAL;
            break token;
          }
          //ロング定数(IOCSコール名,SXコール名,FEファンクションコール名,DOSコール名)
          if (epbConstLongMap.containsKey (lowerIdentifier)) {
            type = ElementType.ETY_FLOAT;
            floatValue = new EFP (epbConstLongMap.get (lowerIdentifier));
            break token;
          }
          if (mode == EVM_ASSEMBLER &&  //アセンブラモードで
              lastToken == null) {  //行頭またはセパレータの直後
            //ラベル定義
            while (c == ':') {  //直後の':'の並びを読み飛ばす
              c = p < l ? a[p++] : -1;
            }
            type = ElementType.ETY_LABEL_DEFINITION;
            stringValue = identifier;  //小文字化されていない方
            break token;
          }
          //変数
          type = (lowerIdentifier.endsWith ("$") ?
                  ElementType.ETY_VARIABLE_STRING :  //"$"で終わっていたら文字列変数
                  ElementType.ETY_VARIABLE_FLOAT);  //それ以外は浮動小数点変数
          stringValue = identifier;  //小文字化されていない方
          break token;
        }  //識別子

        //----------------------------------------
        //演算子
        {  //演算子
          int d = p < l ? a[p] : -1;
          int e = p + 1 < l ? a[p + 1] : -1;
          int f = p + 2 < l ? a[p + 2] : -1;
          if (c == '!') {
            if (d == '=') {  // !=
              p++;
              type = ElementType.ETY_OPERATOR_NOT_EQUAL;
            } else {  // !
              type = ElementType.ETY_TOKEN_EXCLAMATION_MARK;
            }
          } else if (c == '%') {
            if (d == '=') {  // %=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_MODULUS;
            } else {  // %
              type = ElementType.ETY_TOKEN_PERCENT_SIGN;
            }
          } else if (c == '&') {
            if (d == '&') {  // &&
              p++;
              type = ElementType.ETY_OPERATOR_LOGICAL_AND;
            } else if (d == '=') {  // &=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_BITWISE_AND;
            } else {  // &
              type = ElementType.ETY_OPERATOR_BITWISE_AND;
            }
          } else if (c == '#') {
            type = ElementType.ETY_TOKEN_NUMBER_SIGN;
          } else if (c == '(') {
            type = ElementType.ETY_TOKEN_LEFT_PARENTHESIS;
          } else if (c == ')') {
            type = ElementType.ETY_TOKEN_RIGHT_PARENTHESIS;
          } else if (c == '*') {
            if (d == '*') {  // **
              if (e == '=') {  // **=
                p += 2;
                type = ElementType.ETY_OPERATOR_SELF_POWER;
              } else {  // **
                p++;
                type = ElementType.ETY_OPERATOR_POWER;
              }
            } else if (d == '=') {  // *=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_MULTIPLICATION;
            } else {  // *
              type = ElementType.ETY_TOKEN_ASTERISK;
            }
          } else if (c == '+') {
            if (d == '+') {  // ++
              p++;
              type = ElementType.ETY_TOKEN_PLUS_PLUS;
            } else if (d == '=') {  // +=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_ADDITION;
            } else {  // +
              type = ElementType.ETY_TOKEN_PLUS_SIGN;
            }
          } else if (c == ',') {
            type = ElementType.ETY_TOKEN_COMMA;
          } else if (c == '-') {
            if (d == '-') {  // --
              p++;
              type = ElementType.ETY_TOKEN_MINUS_MINUS;
            } else if (d == '=') {  // -=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_SUBTRACTION;
            } else {  // -
              type = ElementType.ETY_TOKEN_HYPHEN_MINUS;
            }
          } else if (c == '.') {
            if (!('0' <= e && e <= '9' || 'A' <= e && e <= 'Z' || 'a' <= e & e <= 'z' ||
                  e == '$' || e == '_')) {
              if (d == 'B' || d == 'b') {  // .b
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_BYTE;
                subscript = 'b';
              } else if (d == 'W' || d == 'w') {  // .w
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_WORD;
                subscript = 'w';
              } else if (d == 'L' || d == 'l') {  // .l
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_LONG;
                subscript = 'l';
              } else if (d == 'Q' || d == 'q') {  // .q
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_QUAD;
                subscript = 'q';
              } else if (d == 'S' || d == 's') {  // .s
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_SINGLE;
                subscript = 's';
              } else if (d == 'D' || d == 'd') {  // .d
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_DOUBLE;
                subscript = 'd';
              } else if (d == 'X' || d == 'x') {  // .x
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_EXTENDED;
                subscript = 'x';
              } else if (d == 'T' || d == 't') {  // .t
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_TRIPLE;
                subscript = 't';
              } else if (d == 'P' || d == 'p') {  // .p
                p++;
                type = ElementType.ETY_OPERATOR_SIZE_PACKED;
                subscript = 'p';
              } else {
                type = ElementType.ETY_TOKEN_FULL_STOP;
              }
            } else {
              type = ElementType.ETY_TOKEN_FULL_STOP;
            }
          } else if (c == '/') {
            if (d == '=') {  // /=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_DIVISION;
            } else {  // /
              type = ElementType.ETY_TOKEN_SOLIDUS;
            }
          } else if (c == ':') {
            type = ElementType.ETY_TOKEN_COLON;
          } else if (c == ';') {
            type = ElementType.ETY_TOKEN_SEMICOLON;
          } else if (c == '<') {
            if (d == '<') {
              if (e == '=') {  // <<=
                p += 2;
                type = ElementType.ETY_OPERATOR_SELF_LEFT_SHIFT;
              } else {  // <<
                p++;
                type = ElementType.ETY_OPERATOR_LEFT_SHIFT;
              }
            } else if (d == '=') {  // <=
              p++;
              type = ElementType.ETY_OPERATOR_LESS_OR_EQUAL;
            } else {  // <
              type = ElementType.ETY_OPERATOR_LESS_THAN;
            }
          } else if (c == '=') {
            if (d == '=') {  // ==
              p++;
              type = ElementType.ETY_OPERATOR_EQUAL;
            } else {  // =
              type = ElementType.ETY_OPERATOR_ASSIGNMENT;
            }
          } else if (c == '>') {
            if (d == '>') {
              if (e == '>') {
                if (f == '=') {  // >>>=
                  p += 3;
                  type = ElementType.ETY_OPERATOR_SELF_UNSIGNED_RIGHT_SHIFT;
                } else {  // >>>
                  p += 2;
                  type = ElementType.ETY_OPERATOR_UNSIGNED_RIGHT_SHIFT;
                }
              } else if (e == '=') {  // >>=
                p += 2;
                type = ElementType.ETY_OPERATOR_SELF_RIGHT_SHIFT;
              } else {  // >>
                p++;
                type = ElementType.ETY_OPERATOR_RIGHT_SHIFT;
              }
            } else if (d == '=') {  // >=
              p++;
              type = ElementType.ETY_OPERATOR_GREATER_OR_EQUAL;
            } else {  // >
              type = ElementType.ETY_OPERATOR_GREATER_THAN;
            }
          } else if (c == '?') {
            type = ElementType.ETY_TOKEN_QUESTION_MARK;
          } else if (c == '@') {
            type = ElementType.ETY_OPERATOR_AT;
          } else if (c == '[') {
            type = ElementType.ETY_TOKEN_LEFT_SQUARE_BRACKET;
          } else if (c == ']') {
            type = ElementType.ETY_TOKEN_RIGHT_SQUARE_BRACKET;
          } else if (c == '^') {
            if (d == '=') {  // ^=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_BITWISE_XOR;
            } else {  // ^
              type = ElementType.ETY_OPERATOR_BITWISE_XOR;
            }
          } else if (c == '{') {
            type = ElementType.ETY_TOKEN_LEFT_CURLY_BRACKET;
          } else if (c == '|') {
            if (d == '|') {  // ||
              p++;
              type = ElementType.ETY_OPERATOR_LOGICAL_OR;
            } else if (d == '=') {  // |=
              p++;
              type = ElementType.ETY_OPERATOR_SELF_BITWISE_OR;
            } else {  // |
              type = ElementType.ETY_OPERATOR_BITWISE_OR;
            }
          } else if (c == '}') {
            type = ElementType.ETY_TOKEN_RIGHT_CURLY_BRACKET;
          } else if (c == '~') {
            type = ElementType.ETY_TOKEN_TILDE;
          } else {
            evxPrintError (Multilingual.mlnJapanese ?
                           "使用できない文字です" :
                           "unusable character",
                           source, p0, 1);
            return null;
          }
          c = p < l ? a[p++] : -1;
        }  //演算子

      }  //token:
      lastToken = new ExpressionElement (type, subscript,
                                         valueType, floatValue, stringValue,
                                         source, p0, (c < 0 ? p : p - 1) - p0);
      tokenList.add (lastToken);
      if (type == ElementType.ETY_TOKEN_SEMICOLON) {
        lastToken = null;
      }
    }  //while 0<=c

    if (false) {
      for (ExpressionElement elem : tokenList) {
        DebugConsole.dgtPrintln (elem.exlType.name ());
      }
    }

    //----------------------------------------------------------------
    //構文解析
    //  トークンリストをノードツリーに変換する
    ExpressionElement nodeTree = evxParseSeparator (tokenList, mode);
    if (nodeTree == null) {
      return null;
    }
    if (!tokenList.isEmpty ()) {  //何か残っている
      ExpressionElement elem = tokenList.peekFirst ();
      evxPrintError (Multilingual.mlnJapanese ?
                     "; がありません" :
                     "; is not found",
                     elem.exlSource, elem.exlOffset, elem.exlLength);
    }

    return nodeTree;

  }  //evxParse


  //--------------------------------------------------------------------------------
  //  基本要素
  protected ExpressionElement evxParsePrimitive (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement elem = tokenList.pollFirst ();
    if (elem == null) {
      return null;
    }
    switch (elem.exlType) {

      //浮動小数点変数
    case ETY_VARIABLE_FLOAT:
      {
        String variableName = elem.exlStringValue;
        ExpressionElement variableBody = evxVariableMap.get (variableName);  //探す
        if (variableBody == null) {
          variableBody = new ExpressionElement (ElementType.ETY_VARIABLE_FLOAT, 0,
                                                ElementType.ETY_FLOAT, new EFP (), "",
                                                elem.exlSource, elem.exlOffset, elem.exlLength);  //作る
          evxVariableMap.put (variableName, variableBody);  //登録する
        }
        elem.exlType = ElementType.ETY_VARIABLE_FLOAT;
        elem.exlValueType = ElementType.ETY_FLOAT;
        elem.exlParamX = variableBody;  //変数の本体
        return elem;
      }

      //文字列変数
    case ETY_VARIABLE_STRING:
      {
        String variableName = elem.exlStringValue;
        ExpressionElement variableBody = evxVariableMap.get (variableName);  //探す
        if (variableBody == null) {
          variableBody = new ExpressionElement (ElementType.ETY_VARIABLE_STRING, 0,
                                                ElementType.ETY_STRING, new EFP (), "",
                                                elem.exlSource, elem.exlOffset, elem.exlLength);  //作る
          evxVariableMap.put (variableName, variableBody);  //登録する
        }
        elem.exlType = ElementType.ETY_VARIABLE_STRING;
        elem.exlValueType = ElementType.ETY_STRING;
        elem.exlParamX = variableBody;  //変数の本体
        return elem;
      }

      //浮動小数点数
    case ETY_FLOAT:
      elem.exlValueType = ElementType.ETY_FLOAT;
      return elem;

      //文字列
    case ETY_STRING:
      elem.exlValueType = ElementType.ETY_STRING;
      return elem;

      //数学定数
    case ETY_MATH_APERY:
    case ETY_MATH_CATALAN:
    case ETY_MATH_NAPIER:
    case ETY_MATH_EULER:
    case ETY_MATH_PI:
      elem.exlValueType = ElementType.ETY_FLOAT;
      return elem;

      //整数レジスタ
    case ETY_INTEGER_REGISTER:
      if (mode != EVM_ASSEMBLER) {  //アセンブラモード以外
        elem.exlValueType = ElementType.ETY_FLOAT;
        return elem;
      }
      //アセンブラモード
      {
        ExpressionElement colon = tokenList.peekFirst ();  // :または-または/
        if (colon == null) {
          return elem;
        }
        ExpressionElement dh = elem;  // Dh
        ExpressionElement dl = dh;  //Dl
        int h = dh.exlSubscript;
        int l = h;
        //整数レジスタリスト
        if (colon.exlType == ElementType.ETY_TOKEN_HYPHEN_MINUS ||
            colon.exlType == ElementType.ETY_TOKEN_SOLIDUS) {  // -または/。整数レジスタリスト
          if (colon.exlType == ElementType.ETY_TOKEN_HYPHEN_MINUS) {  // -
            tokenList.pollFirst ();  // -
            dl = tokenList.peekFirst ();  // Dl
            if (dl == null || dl.exlType != ElementType.ETY_INTEGER_REGISTER) {  // -の後にDlがない
              evxPrintError (Multilingual.mlnJapanese ?
                             "不完全なレジスタリスト" :
                             "incomplete register list",
                             elem.exlSource, elem.exlOffset, colon.exlOffset + colon.exlLength - elem.exlOffset);
              return null;
            }
            tokenList.pollFirst ();  // Dl
            l = dl.exlSubscript;
            if (l <= h) {  //昇順でない。データレジスタとアドレスレジスタを跨いでいるレジスタリストは許容する
              evxPrintError (Multilingual.mlnJapanese ?
                             "昇順でないレジスタリスト" :
                             "register list not in ascending order",
                             elem.exlSource, elem.exlOffset, dl.exlOffset + dl.exlLength - elem.exlOffset);
              return null;
            }
            colon = tokenList.peekFirst ();  // /
          }
          int subscript = (2 << l) - (1 << h);
          while (colon != null && colon.exlType == ElementType.ETY_TOKEN_SOLIDUS) {  // /
            tokenList.pollFirst ();  // /
            dh = tokenList.peekFirst ();  // Dh
            if (dh == null || dh.exlType != ElementType.ETY_INTEGER_REGISTER) {  // /の後にDhがない
              evxPrintError (Multilingual.mlnJapanese ?
                             "不完全なレジスタリスト" :
                             "incomplete register list",
                             elem.exlSource, elem.exlOffset, colon.exlOffset + colon.exlLength - elem.exlOffset);
              return null;
            }
            tokenList.pollFirst ();  // Dh
            h = dh.exlSubscript;
            dl = dh;
            l = dl.exlSubscript;
            colon = tokenList.peekFirst ();  // -または/
            if (colon != null && colon.exlType == ElementType.ETY_TOKEN_HYPHEN_MINUS) {  // -
              tokenList.pollFirst ();  // -
              dl = tokenList.peekFirst ();  // Dl
              if (dl == null || dl.exlType != ElementType.ETY_INTEGER_REGISTER) {  // -の後にDlがない
                evxPrintError (Multilingual.mlnJapanese ?
                               "不完全なレジスタリスト" :
                               "incomplete register list",
                               elem.exlSource, elem.exlOffset, colon.exlOffset + colon.exlLength - elem.exlOffset);
                return null;
              }
              tokenList.pollFirst ();  // Dl
              l = dl.exlSubscript;
              if (l <= h) {  //昇順でない。データレジスタとアドレスレジスタを跨いでいるレジスタリストは許容する
                evxPrintError (Multilingual.mlnJapanese ?
                               "昇順でないレジスタリスト" :
                               "register list not in ascending order",
                               elem.exlSource, elem.exlOffset, dl.exlOffset + dl.exlLength - elem.exlOffset);
                return null;
              }
              colon = tokenList.peekFirst ();  // /
            }
            int mask = (2 << l) - (1 << h);
            if ((subscript & mask) != 0) {  //重複している
              evxPrintError (Multilingual.mlnJapanese ?
                             "レジスタが重複しています" :
                             "duplicated register",
                             elem.exlSource, elem.exlOffset, dl.exlOffset + dl.exlLength - elem.exlOffset);
              return null;
            }
            subscript |= mask;
          }  //while
          return new ExpressionElement (
            ElementType.ETY_INTEGER_REGISTER_LIST, subscript,
            ElementType.ETY_UNDEF, null, null,
            elem.exlSource, elem.exlOffset, dl.exlOffset + dl.exlLength - elem.exlOffset,
            null, null, null);
        }  //if 整数レジスタリスト
      }
      return elem;

      //浮動小数点レジスタ
    case ETY_FLOATING_POINT_REGISTER:
      if (mode != EVM_ASSEMBLER) {  //アセンブラモード以外
        elem.exlValueType = ElementType.ETY_FLOAT;
        return elem;
      }
      //アセンブラモード
      {
        ExpressionElement hyphen = tokenList.peekFirst ();  // -または/
        if (hyphen == null) {
          return elem;
        }
        ExpressionElement fpm = elem;  // FPm
        int m = fpm.exlSubscript;
        ExpressionElement fpn = fpm;  //FPn
        int n = fpn.exlSubscript;
        //浮動小数点レジスタリスト
        if (hyphen.exlType == ElementType.ETY_TOKEN_HYPHEN_MINUS ||
            hyphen.exlType == ElementType.ETY_TOKEN_SOLIDUS) {  // -または/。浮動小数点レジスタリスト
          if (hyphen.exlType == ElementType.ETY_TOKEN_HYPHEN_MINUS) {  // -
            tokenList.pollFirst ();  // -
            fpn = tokenList.peekFirst ();  // FPn
            if (fpn == null || fpn.exlType != ElementType.ETY_FLOATING_POINT_REGISTER) {  // -の後にFPnがない
              evxPrintError (Multilingual.mlnJapanese ?
                             "不完全なレジスタリスト" :
                             "incomplete register list",
                             elem.exlSource, elem.exlOffset, hyphen.exlOffset + hyphen.exlLength - elem.exlOffset);
              return null;
            }
            tokenList.pollFirst ();  // FPn
            n = fpn.exlSubscript;
            if (n <= m) {  //昇順でない
              evxPrintError (Multilingual.mlnJapanese ?
                             "昇順でないレジスタリスト" :
                             "register list not in ascending order",
                             elem.exlSource, elem.exlOffset, fpn.exlOffset + fpn.exlLength - elem.exlOffset);
              return null;
            }
            hyphen = tokenList.peekFirst ();  // /
          }
          int subscript = (2 << n) - (1 << m);
          while (hyphen != null && hyphen.exlType == ElementType.ETY_TOKEN_SOLIDUS) {  // /
            tokenList.pollFirst ();  // /
            fpm = tokenList.peekFirst ();  // FPm
            if (fpm == null || fpm.exlType != ElementType.ETY_FLOATING_POINT_REGISTER) {  // /の後にFPmがない
              evxPrintError (Multilingual.mlnJapanese ?
                             "不完全なレジスタリスト" :
                             "incomplete register list",
                             elem.exlSource, elem.exlOffset, hyphen.exlOffset + hyphen.exlLength - elem.exlOffset);
              return null;
            }
            tokenList.pollFirst ();  // FPm
            m = fpm.exlSubscript;
            fpn = fpm;
            n = fpn.exlSubscript;
            hyphen = tokenList.peekFirst ();  // -または/
            if (hyphen != null && hyphen.exlType == ElementType.ETY_TOKEN_HYPHEN_MINUS) {  // -
              tokenList.pollFirst ();  // -
              fpn = tokenList.peekFirst ();  // FPn
              if (fpn == null || fpn.exlType != ElementType.ETY_FLOATING_POINT_REGISTER) {  // -の後にFPnがない
                evxPrintError (Multilingual.mlnJapanese ?
                               "不完全なレジスタリスト" :
                               "incomplete register list",
                               elem.exlSource, elem.exlOffset, hyphen.exlOffset + hyphen.exlLength - elem.exlOffset);
                return null;
              }
              tokenList.pollFirst ();  // FPn
              n = fpn.exlSubscript;
              if (n <= m) {  //昇順でない
                evxPrintError (Multilingual.mlnJapanese ?
                               "昇順でないレジスタリスト" :
                               "register list not in ascending order",
                               elem.exlSource, elem.exlOffset, fpn.exlOffset + fpn.exlLength - elem.exlOffset);
                return null;
              }
              hyphen = tokenList.peekFirst ();  // /
            }
            int mask = (2 << n) - (1 << m);
            if ((subscript & mask) != 0) {  //重複している
              evxPrintError (Multilingual.mlnJapanese ?
                             "浮動小数点レジスタが重複しています" :
                             "duplicated floating point register",
                             elem.exlSource, elem.exlOffset, fpn.exlOffset + fpn.exlLength - elem.exlOffset);
              return null;
            }
            subscript |= mask;
          }  //while
          return new ExpressionElement (
            ElementType.ETY_FLOATING_POINT_REGISTER_LIST, subscript,
            ElementType.ETY_UNDEF, null, null,
            elem.exlSource, elem.exlOffset, fpn.exlOffset + fpn.exlLength - elem.exlOffset,
            null, null, null);
        }  //if 浮動小数点レジスタリスト
      }
      return elem;

      //制御レジスタ
    case ETY_PC:
    case ETY_CCR:
    case ETY_SR:
    case ETY_CONTROL_REGISTER:
      if (mode != EVM_ASSEMBLER) {  //アセンブラモード以外
        elem.exlValueType = ElementType.ETY_FLOAT;
        return elem;
      }
      //アセンブラモード
      return elem;

      //浮動小数点制御レジスタ
    case ETY_FLOAT_CONTROL_REGISTER:
      if (mode != EVM_ASSEMBLER) {  //アセンブラモード以外
        elem.exlValueType = ElementType.ETY_FLOAT;
        return elem;
      }
      //アセンブラモード
      {
        ExpressionElement cr = elem;  //FPcr
        //浮動小数点制御レジスタは1個だけでも浮動小数点制御レジスタリストになる
        int subscript = cr.exlSubscript;
        ExpressionElement solidus = tokenList.peekFirst ();  // /
        while (solidus != null && solidus.exlType == ElementType.ETY_TOKEN_SOLIDUS) {  // /
          tokenList.pollFirst ();  // /
          cr = tokenList.peekFirst ();  // FPcr
          if (cr == null || cr.exlType != ElementType.ETY_FLOAT_CONTROL_REGISTER) {  // /の後にFPcrがない
            evxPrintError (Multilingual.mlnJapanese ?
                           "不完全な浮動小数点制御レジスタリスト" :
                           "incomplete floating point control register list",
                           elem.exlSource, elem.exlOffset, solidus.exlOffset + solidus.exlLength - elem.exlOffset);
            return null;
          }
          tokenList.pollFirst ();  // FPcr
          int mask = cr.exlSubscript;
          if ((subscript & mask) != 0) {  //重複している
            evxPrintError (Multilingual.mlnJapanese ?
                           "浮動小数点制御レジスタが重複しています" :
                           "duplicated floating point control register",
                           elem.exlSource, elem.exlOffset, cr.exlOffset + cr.exlLength - elem.exlOffset);
            return null;
          }
          subscript |= mask;
          solidus = tokenList.peekFirst ();  // /
        }  //while /
        return new ExpressionElement (
          ElementType.ETY_FLOATING_POINT_CONTROL_REGISTER_LIST, subscript,
          ElementType.ETY_UNDEF, null, null,
          elem.exlSource, elem.exlOffset, cr.exlOffset + cr.exlLength - elem.exlOffset,
          null, null, null);
      }

      //関数
    case ETY_FUNCTION_ABS:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ACOS:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ACOSH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ACOT:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ACOTH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ACSC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ACSCH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_AGI:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_AGM:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ASC:
      return evxParseFunctionFloatString (elem, tokenList, mode);
    case ETY_FUNCTION_ASEC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ASECH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ASIN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ASINH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ATAN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ATAN2:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ATANH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_BIN_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CBRT:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CEIL:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CHR_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CMP:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CMP0:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CMP1:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CMP1ABS:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CMPABS:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_COS:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_COSH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_COT:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_COTH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CSC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CSCH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_CUB:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_DEC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_DEG:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_DIV2:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_DIV3:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_DIVPI:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_DIVRZ:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_EXP:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_EXP10:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_EXP2:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_EXP2M1:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_EXPM1:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_FLOOR:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_FRAC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_GETEXP:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_GETMAN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_HEX_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList, mode);
    case ETY_FUNCTION_IEEEREM:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_INC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ISEVEN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ISINF:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ISINT:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ISNAN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ISODD:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ISONE:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ISZERO:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_LGAMMA:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_LOG:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_LOG10:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_LOG1P:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_LOG2:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_MAX:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_MIN:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_MUL2:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_MUL3:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_MULPI:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_OCT_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList, mode);
    case ETY_FUNCTION_POW:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_QUO:
      return evxParseFunctionFloatFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_RAD:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_RANDOM:
      return evxParseFunctionFloat (elem, tokenList, mode);
    case ETY_FUNCTION_RCP:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_RINT:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_RMODE:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ROUND:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_RPREC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_SEC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_SECH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_SGN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_SIN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_SINH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_SQRT:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_SQU:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_STR_DOLLAR:
      return evxParseFunctionStringFloat (elem, tokenList, mode);
    case ETY_FUNCTION_TAN:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_TANH:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_TGAMMA:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_TRUNC:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_ULP:
      return evxParseFunctionFloatFloat (elem, tokenList, mode);
    case ETY_FUNCTION_VAL:
      return evxParseFunctionFloatString (elem, tokenList, mode);

      //丸括弧
    case ETY_TOKEN_LEFT_PARENTHESIS:  // (x)
      {
        ExpressionElement paramX = evxParseComma (tokenList, mode);  //最も優先順位の低い演算子
        if (paramX == null) {
          return null;
        }
        ExpressionElement right = tokenList.peekFirst ();  // )
        if (right == null || right.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  // (xの後に)がない
          evxPrintError (Multilingual.mlnJapanese ?
                         "(...) が閉じていません" :
                         "(...) is not closed",
                         elem.exlSource, elem.exlOffset, paramX.exlOffset + paramX.exlLength - elem.exlOffset);
          return null;
        }
        tokenList.pollFirst ();  // )
        if (paramX.exlValueType == ElementType.ETY_FLOAT ||
            paramX.exlValueType == ElementType.ETY_STRING) {
          return paramX;
        }
        if (mode != EVM_ASSEMBLER) {  //アセンブラモード以外
          evxPrintError (Multilingual.mlnJapanese ?
                         "引数の型が違います" :
                         "wrong type of parameter",
                         elem.exlSource, elem.exlOffset, right.exlOffset + right.exlLength - elem.exlOffset);
          return null;
        }
        //アセンブラモード
        if (paramX.exlType == ElementType.ETY_INTEGER_REGISTER) {  // (Rr)。レジスタ間接
          return new ExpressionElement (
            ElementType.ETY_REGISTER_INDIRECT, paramX.exlSubscript,
            ElementType.ETY_UNDEF, null, null,
            elem.exlSource, elem.exlOffset, right.exlOffset + right.exlLength - elem.exlOffset,
            null, null, null);
        }
        return new ExpressionElement (
          ElementType.ETY_PARENTHESIS, 0,
          ElementType.ETY_UNDEF, null, null,
          elem.exlSource, elem.exlOffset, right.exlOffset + right.exlLength - elem.exlOffset,
          paramX, null, null);
      }

      //角括弧
    case ETY_TOKEN_LEFT_SQUARE_BRACKET:  // [x]
      {
        ExpressionElement paramX = evxParseComma (tokenList, mode);  //最も優先順位の低い演算子
        if (paramX == null) {
          return null;
        }
        ExpressionElement right = tokenList.pollFirst ();  // ]
        if (right == null ||
            right.exlType != ElementType.ETY_TOKEN_RIGHT_SQUARE_BRACKET) {  //]がない
          evxPrintError (Multilingual.mlnJapanese ?
                         "[...] が閉じていません" :
                         "[...] is not closed",
                         elem.exlSource, elem.exlOffset, paramX.exlOffset + paramX.exlLength - elem.exlOffset);
          return null;
        }
        if (mode != EVM_ASSEMBLER) {
          if (paramX.exlValueType != ElementType.ETY_FLOAT) {
            evxPrintError (Multilingual.mlnJapanese ?
                           "アドレスの型が違います" :
                           "wrong type of address",
                           elem.exlSource, elem.exlOffset, right.exlOffset + right.exlLength - elem.exlOffset);
            return null;
          }
        }
        return new ExpressionElement (
          ElementType.ETY_SQUARE_BRACKET, 0,
          mode == EVM_ASSEMBLER ? ElementType.ETY_UNDEF : ElementType.ETY_FLOAT, null, "",
          elem.exlSource, elem.exlOffset, right.exlOffset + right.exlLength - elem.exlOffset,
          paramX, null, null);
      }

    }  //switch exlType

    if (mode == EVM_ASSEMBLER) {  //アセンブラモード
      return elem;
    }

    evxPrintError (Multilingual.mlnJapanese ?
                   "文法エラー" :
                   "syntax error",
                   elem.exlSource, elem.exlOffset, elem.exlLength);
    return null;

  }  //evxParsePrimitive


  //--------------------------------------------------------------------------------
  //  0引数関数
  protected ExpressionElement evxParseFunctionFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList, int mode) {
    return evxParseFunction0 (elem, tokenList,
                              ElementType.ETY_FLOAT, mode);
  }  //evxParseFunctionFloat
  protected ExpressionElement evxParseFunctionString (ExpressionElement elem, LinkedList<ExpressionElement> tokenList, int mode) {
    return evxParseFunction0 (elem, tokenList,
                              ElementType.ETY_STRING, mode);
  }  //evxParseFunctionString
  protected ExpressionElement evxParseFunction0 (ExpressionElement elem, LinkedList<ExpressionElement> tokenList,
                                                 ElementType valueType, int mode) {
    ExpressionElement commaOrParen = tokenList.pollFirst ();  //(
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_LEFT_PARENTHESIS) {  //(がない
      evxPrintError (Multilingual.mlnJapanese ?
                     "( がありません" :
                     "( is not found",
                     elem.exlSource, -1, 1);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //)
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  //)がない
      evxPrintError (Multilingual.mlnJapanese ?
                     ") がありません" :
                     ") is not found",
                     elem.exlSource, -1, 1);
      return null;
    }
    elem.exlValueType = valueType;
    elem.exlParamX = null;
    elem.exlParamY = null;
    elem.exlParamZ = null;
    elem.exlLength = commaOrParen.exlOffset + commaOrParen.exlLength - elem.exlOffset;
    return elem;
  }  //evxParseFunction0


  //--------------------------------------------------------------------------------
  //  1引数関数
  protected ExpressionElement evxParseFunctionFloatFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList, int mode) {
    return evxParseFunction1 (elem, tokenList,
                              ElementType.ETY_FLOAT, ElementType.ETY_FLOAT, mode);
  }  //evxParseFunctionFloatFloat
  protected ExpressionElement evxParseFunctionFloatString (ExpressionElement elem, LinkedList<ExpressionElement> tokenList, int mode) {
    return evxParseFunction1 (elem, tokenList,
                              ElementType.ETY_FLOAT, ElementType.ETY_STRING, mode);
  }  //evxParseFunctionFloatString
  protected ExpressionElement evxParseFunctionStringFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList, int mode) {
    return evxParseFunction1 (elem, tokenList,
                              ElementType.ETY_STRING, ElementType.ETY_FLOAT, mode);
  }  //evxParseFunctionStringFloat
  protected ExpressionElement evxParseFunction1 (ExpressionElement elem, LinkedList<ExpressionElement> tokenList,
                                                 ElementType valueType, ElementType paramTypeX, int mode) {
    ExpressionElement commaOrParen = tokenList.pollFirst ();  //(
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_LEFT_PARENTHESIS) {  //(がない
      evxPrintError (Multilingual.mlnJapanese ?
                     "( がありません" :
                     "( is not found",
                     elem.exlSource, -1, 1);
      return null;
    }
    ExpressionElement paramX = evxParseAssignment (tokenList, mode);  //コンマ演算子の次に優先順位の低い演算子
    if (paramX == null) {
      return null;
    }
    if (paramX.exlValueType != paramTypeX) {  //1番目の引数の型が違う
      evxPrintError (Multilingual.mlnJapanese ?
                     "1 番目の引数の型が違います" :
                     "wrong type of the 1st parameter",
                     paramX.exlSource, paramX.exlOffset, paramX.exlLength);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //)
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  //)がない
      evxPrintError (Multilingual.mlnJapanese ?
                     ") がありません" :
                     ") is not found",
                     elem.exlSource, -1, 1);
      return null;
    }
    elem.exlValueType = valueType;
    elem.exlParamX = paramX;
    elem.exlParamY = null;
    elem.exlParamZ = null;
    elem.exlLength = commaOrParen.exlOffset + commaOrParen.exlLength - elem.exlOffset;
    return elem;
  }  //evxParseFunction1


  //--------------------------------------------------------------------------------
  //  2引数関数
  protected ExpressionElement evxParseFunctionFloatFloatFloat (ExpressionElement elem, LinkedList<ExpressionElement> tokenList, int mode) {
    return evxParseFunction2 (elem, tokenList,
                              ElementType.ETY_FLOAT, ElementType.ETY_FLOAT, ElementType.ETY_FLOAT, mode);
  }  //evxParseFunctionFloatFloatFloat
  protected ExpressionElement evxParseFunction2 (ExpressionElement elem, LinkedList<ExpressionElement> tokenList,
                                                 ElementType valueType, ElementType paramTypeX, ElementType paramTypeY, int mode) {
    ExpressionElement commaOrParen = tokenList.pollFirst ();  //(
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_LEFT_PARENTHESIS) {  //(がない
      evxPrintError (Multilingual.mlnJapanese ?
                     "( がありません" :
                     "( is not found",
                     elem.exlSource, -1, 1);
      return null;
    }
    ExpressionElement paramX = evxParseAssignment (tokenList, mode);  //コンマ演算子の次に優先順位の低い演算子
    if (paramX == null) {
      return null;
    }
    if (paramX.exlValueType != paramTypeX) {  //1番目の引数の型が違う
      evxPrintError (Multilingual.mlnJapanese ?
                     "1 番目の引数の型が違います" :
                     "wrong type of the 1st parameter",
                     paramX.exlSource, paramX.exlOffset, paramX.exlLength);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //,
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_OPERATOR_COMMA) {  //,がない
      evxPrintError (Multilingual.mlnJapanese ?
                     ", がありません" :
                     ", is not found",
                     elem.exlSource, -1, 1);
      return null;
    }
    ExpressionElement paramY = evxParseAssignment (tokenList, mode);  //コンマ演算子の次に優先順位の低い演算子
    if (paramY == null) {
      return null;
    }
    if (paramX.exlValueType != paramTypeY) {  //2番目の引数の型が違う
      evxPrintError (Multilingual.mlnJapanese ?
                     "2 番目の引数の型が違います" :
                     "wrong type of the 2nd parameter",
                     paramY.exlSource, paramY.exlOffset, paramY.exlLength);
      return null;
    }
    commaOrParen = tokenList.pollFirst ();  //)
    if (commaOrParen == null || commaOrParen.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  //)がない
      evxPrintError (Multilingual.mlnJapanese ?
                     ") がありません" :
                     ") is not found",
                     elem.exlSource, -1, 1);
      return null;
    }
    elem.exlValueType = valueType;
    elem.exlParamX = paramX;
    elem.exlParamY = paramY;
    elem.exlParamZ = null;
    elem.exlLength = commaOrParen.exlOffset + commaOrParen.exlLength - elem.exlOffset;
    return elem;
  }  //evxParseFunction2


  //--------------------------------------------------------------------------------
  //  ＠演算子
  protected ExpressionElement evxParseAt (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParsePrimitive (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_AT:  // x@y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParsePrimitive (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "アドレスの型が違います" :
                       "wrong type of the address",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "ファンクションコードの型が違います" :
                       "wrong type of the function code",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseAt


  //--------------------------------------------------------------------------------
  //  後置演算子
  protected ExpressionElement evxParsePostfix (LinkedList<ExpressionElement> tokenList, int mode) {

    ExpressionElement paramX = evxParseAt (tokenList, mode);
    if (paramX == null) {
      return null;
    }

    for (ExpressionElement operator = tokenList.peekFirst ();
         operator != null;
         operator = tokenList.peekFirst ()) {

      //ポストインクリメントまたはポストデクリメント
      //    x++
      //    x--
      if (operator.exlType == ElementType.ETY_TOKEN_PLUS_PLUS ||  // x++
          operator.exlType == ElementType.ETY_TOKEN_MINUS_MINUS) {  // x--
        tokenList.pollFirst ();  // ++または--
        if (paramX.exlValueType != ElementType.ETY_FLOAT) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "引数の型が違います" :
                         "wrong type of parameter",
                         operator.exlSource, operator.exlOffset, operator.exlLength);
          return null;
        }
        if (mode == EVM_ASSEMBLER) {  //アセンブラモード
          evxPrintError (Multilingual.mlnJapanese ?
                         "副作用を起こす演算子はここでは使えません" :
                         "operators which cause a side effect is unusable here",
                         operator.exlSource, operator.exlOffset, operator.exlLength);
          return null;
        }
        if (!paramX.exlIsFloatSubstituend ()) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "引数が場所を示していません" :
                         "parameter is not indicating a location",
                         operator.exlSource, operator.exlOffset, operator.exlLength);
          return null;
        }
        paramX = new ExpressionElement (
          operator.exlType == ElementType.ETY_TOKEN_PLUS_PLUS ?
          ElementType.ETY_OPERATOR_POSTINCREMENT :
          ElementType.ETY_OPERATOR_POSTDECREMENT, 0,
          ElementType.ETY_FLOAT, null, null,
          paramX.exlSource, paramX.exlOffset, operator.exlOffset + operator.exlLength - paramX.exlOffset,
          paramX, null, null);
        continue;
      }  //if x++ x--

      if (mode != EVM_ASSEMBLER) {  //アセンブラモード以外
        break;
      }

      //アセンブラモード

      //ディスプレースメントまたはインデックスレジスタのサイズ
      //    x.w
      //    x.l
      if ((operator.exlType == ElementType.ETY_OPERATOR_SIZE_WORD ||
           operator.exlType == ElementType.ETY_OPERATOR_SIZE_LONG) &&  // .wまたは.l
          (paramX.exlValueType == ElementType.ETY_FLOAT ||  //ディスプレースメント
           paramX.exlType == ElementType.ETY_INTEGER_REGISTER)) {  //インデックスレジスタ
        tokenList.pollFirst ();  // .wまたは.l
        paramX = new ExpressionElement (
          ElementType.ETY_SIZE, operator.exlSubscript,
          ElementType.ETY_UNDEF, null, null,
          paramX.exlSource, paramX.exlOffset, operator.exlOffset + operator.exlLength - paramX.exlOffset,
          paramX, null, null);
        continue;
      }  //if x.wまたはx.l

      //括弧の左側に書かれたディスプレースメント
      //  浮動小数点数またはサイズ付き浮動小数点数の直後に(があるときディスプレースメントとみなして括弧の中に入れる
      //    x() → (x)  この括弧は省略できない括弧なので(x,za0,zd0.w*1)になる
      //    x(y) → (x,y)
      //    x(y,z) → (x,y,z)
      if ((paramX.exlValueType == ElementType.ETY_FLOAT ||  // x
           (paramX.exlType == ElementType.ETY_SIZE &&
            paramX.exlParamX.exlValueType == ElementType.ETY_FLOAT)) &&  // x.wまたはx.l
          operator.exlType == ElementType.ETY_TOKEN_LEFT_PARENTHESIS) {  // x(またはx.w(またはx.l(
        tokenList.pollFirst ();  // (
        ExpressionElement commaRight = tokenList.peekFirst ();  // )
        if (commaRight != null && commaRight.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  // x(の直後に)以外のものがある
          for (;;) {
            //右辺
            ExpressionElement paramY = evxParseColon (tokenList, mode);
            if (paramY == null) {
              return null;
            }
            paramX = new ExpressionElement (
              ElementType.ETY_OPERATOR_COMMA, 0,
              paramY.exlValueType, null, null,
              paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
              paramX, paramY, null);
            commaRight = tokenList.peekFirst ();  // ,または)
            if (commaRight == null || commaRight.exlType != ElementType.ETY_TOKEN_COMMA) {  // ,が続いていない
              break;
            }
            tokenList.pollFirst ();  // ,
          }
        }
        if (commaRight == null || commaRight.exlType != ElementType.ETY_TOKEN_RIGHT_PARENTHESIS) {  // )がない
          evxPrintError (Multilingual.mlnJapanese ?
                         "(...) が閉じていません" :
                         "(...) is not closed",
                         paramX.exlSource, paramX.exlOffset, paramX.exlLength);
          return null;
        }
        tokenList.pollFirst ();  // )
        paramX = new ExpressionElement (
          ElementType.ETY_PARENTHESIS, 0,
          ElementType.ETY_UNDEF, null, null,
          paramX.exlSource, paramX.exlOffset, commaRight.exlOffset + commaRight.exlLength - paramX.exlOffset,
          paramX, null, null);
        continue;
      }  //if x(またはx.w(またはx.l(

      //ビットフィールドまたはk-factor
      //    <ea>{offset:width}
      //    <ea>{k-factor}
      if (operator.exlType == ElementType.ETY_TOKEN_LEFT_CURLY_BRACKET) {  // <ea>{
        tokenList.pollFirst ();  // {
        ExpressionElement paramY = evxParseAssignment (tokenList, mode);  // offsetまたはk-factor
        if (paramY == null) {
          return null;
        }
        ExpressionElement colon = tokenList.peekFirst ();  // :または}
        if (colon == null) {  // <ea>{offset:または<ea>{k-factor}のいずれでもない
          evxPrintError (Multilingual.mlnJapanese ?
                         "不完全なビットフィールドまたは k-factor" :
                         "incomplete bit-field or k-factor",
                         operator.exlSource, operator.exlOffset, paramY.exlOffset + paramY.exlLength - operator.exlOffset);
          return null;
        }
        tokenList.pollFirst ();  // :または}
        if (colon.exlType == ElementType.ETY_TOKEN_COLON) {  // <ea>{offset:
          //ビットフィールド
          ExpressionElement paramZ = evxParseAssignment (tokenList, mode);  // width
          if (paramZ == null) {
            return null;
          }
          ExpressionElement right = tokenList.peekFirst ();  // }
          if (right == null || right.exlType != ElementType.ETY_TOKEN_RIGHT_CURLY_BRACKET) {  // <ea>{offset:widthの後に}がない
            evxPrintError (Multilingual.mlnJapanese ?
                           "不完全なビットフィールド" :
                           "incomplete bit-field",
                           operator.exlSource, operator.exlOffset, paramZ.exlOffset + paramZ.exlLength - operator.exlOffset);
            return null;
          }
          tokenList.pollFirst ();  // }
          if (!(paramY.exlValueType == ElementType.ETY_FLOAT ||  //浮動小数点数または
                paramY.exlType == ElementType.ETY_IMMEDIATE ||  //イミディエイトまたは
                (paramY.exlType == ElementType.ETY_INTEGER_REGISTER && paramY.exlSubscript < 8))) {  //データレジスタのいずれでもない
            evxPrintError (Multilingual.mlnJapanese ?
                           "ビットフィールドのオフセットの型が違います" :
                           "wrong type of bit-field offset",
                           paramY.exlSource, paramY.exlOffset, paramY.exlLength);
          }
          if (!(paramZ.exlValueType == ElementType.ETY_FLOAT ||  //浮動小数点数または
                paramZ.exlType == ElementType.ETY_IMMEDIATE ||  //イミディエイトまたは
                (paramZ.exlType == ElementType.ETY_INTEGER_REGISTER && paramZ.exlSubscript < 8))) {  //データレジスタのいずれでもない
            evxPrintError (Multilingual.mlnJapanese ?
                           "ビットフィールドの幅の型が違います" :
                           "wrong type of bit-field width",
                           paramZ.exlSource, paramZ.exlOffset, paramZ.exlLength);
          }
          paramX = new ExpressionElement (
            ElementType.ETY_BIT_FIELD, 0,
            ElementType.ETY_UNDEF, null, null,
            paramX.exlSource, paramX.exlOffset, right.exlOffset + right.exlLength - paramX.exlOffset,
            paramX, paramY, paramZ);
          continue;
        }
        if (colon.exlType == ElementType.ETY_TOKEN_RIGHT_CURLY_BRACKET) {  // <ea>{k-factor}
          //k-factor
          if (!(paramY.exlValueType == ElementType.ETY_FLOAT ||  //浮動小数点数または
                paramY.exlType == ElementType.ETY_IMMEDIATE ||  //イミディエイトまたは
                (paramY.exlType == ElementType.ETY_INTEGER_REGISTER && paramY.exlSubscript < 8))) {  //データレジスタのいずれでもない
            evxPrintError (Multilingual.mlnJapanese ?
                           "k-factor の型が違います" :
                           "wrong type of k-factor",
                           paramY.exlSource, paramY.exlOffset, paramY.exlLength);
          }
          paramX = new ExpressionElement (
            ElementType.ETY_K_FACTOR, 0,
            ElementType.ETY_UNDEF, null, null,
            paramX.exlSource, paramX.exlOffset, colon.exlOffset + colon.exlLength - paramX.exlOffset,
            paramX, paramY, null);
          continue;
        }
        // <ea>{offset:または<ea>{k-factor}のいずれでもない
        evxPrintError (Multilingual.mlnJapanese ?
                       "不完全なビットフィールドまたは k-factor" :
                       "incomplete bit-field or k-factor",
                       operator.exlSource, operator.exlOffset, paramY.exlOffset + paramY.exlLength - operator.exlOffset);
        return null;
      }  //if <ea>{

      break;
    }  //for

    return paramX;

  }  //evxParsePostfix


  //--------------------------------------------------------------------------------
  //  前置演算子
  protected ExpressionElement evxParsePrefix (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement operator = tokenList.peekFirst ();
    if (operator == null) {
      return null;
    }
    ElementType type = operator.exlType;
    if (type == ElementType.ETY_TOKEN_PLUS_PLUS) {  // ++x
      type = ElementType.ETY_OPERATOR_PREINCREMENT;
    } else if (type == ElementType.ETY_TOKEN_MINUS_MINUS) {  // --x
      type = ElementType.ETY_OPERATOR_PREDECREMENT;
    } else if (type == ElementType.ETY_TOKEN_PLUS_SIGN) {  // +x
      type = ElementType.ETY_OPERATOR_NOTHING;
    } else if (type == ElementType.ETY_TOKEN_HYPHEN_MINUS) {  //-x
      type = ElementType.ETY_OPERATOR_NEGATION;
    } else if (type == ElementType.ETY_TOKEN_TILDE) {  // ~x
      type = ElementType.ETY_OPERATOR_BITWISE_NOT;
    } else if (type == ElementType.ETY_TOKEN_EXCLAMATION_MARK) {  // !x
      type = ElementType.ETY_OPERATOR_LOGICAL_NOT;
    } else if (mode == EVM_ASSEMBLER &&  //アセンブラモード
               type == ElementType.ETY_TOKEN_NUMBER_SIGN) {  // #x
      type = ElementType.ETY_IMMEDIATE;
    } else {
      return evxParsePostfix (tokenList, mode);
    }
    tokenList.pollFirst ();
    ExpressionElement paramX = evxParsePrefix (tokenList, mode);  //右から結合するので自分を呼ぶ
    if (paramX == null) {
      return null;
    }
    if (mode == EVM_ASSEMBLER &&  //アセンブラモード
        type == ElementType.ETY_OPERATOR_NEGATION &&
        paramX.exlType == ElementType.ETY_REGISTER_INDIRECT &&
        8 <= paramX.exlSubscript) {  // -(Ar)
      return new ExpressionElement (
        ElementType.ETY_PREDECREMENT, paramX.exlSubscript - 8,
        ElementType.ETY_UNDEF, null, null,
        operator.exlSource, operator.exlOffset, paramX.exlOffset + paramX.exlLength - operator.exlOffset,
        null, null, null);
    }
    if (paramX.exlValueType != ElementType.ETY_FLOAT) {
      evxPrintError (Multilingual.mlnJapanese ?
                     "引数の型が違います" :
                     "wrong type of parameter",
                     operator.exlSource, operator.exlOffset, operator.exlLength);
      return null;
    }
    if (type == ElementType.ETY_OPERATOR_PREINCREMENT ||  // ++x
        type == ElementType.ETY_OPERATOR_PREDECREMENT) {  // --x
      if (mode == EVM_ASSEMBLER) {  //アセンブラモード
        evxPrintError (Multilingual.mlnJapanese ?
                       "副作用を起こす演算子はここでは使えません" :
                       "operators which cause a side effect is unusable here",
                       operator.exlSource, operator.exlOffset, operator.exlLength);
        return null;
      }
      if (!paramX.exlIsFloatSubstituend ()) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "引数が場所を示していません" :
                       "parameter is not indicating a location",
                       operator.exlSource, operator.exlOffset, operator.exlLength);
        return null;
      }
    }
    return new ExpressionElement (
      type, 0,
      ElementType.ETY_FLOAT, null, null,
      operator.exlSource, operator.exlOffset, paramX.exlOffset + paramX.exlLength - operator.exlOffset,
      paramX, null, null);
  }  //evxParsePrefix


  //--------------------------------------------------------------------------------
  //  累乗演算子
  protected ExpressionElement evxParseExponentiation (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParsePrefix (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_POWER:  // x**y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseExponentiation (tokenList, mode);  //右から結合するので自分を呼ぶ
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      return elem;
    }
    return paramX;
  }  //evxParseExponentiation


  //--------------------------------------------------------------------------------
  //  乗除算演算子
  protected ExpressionElement evxParseMultiplication (LinkedList<ExpressionElement> tokenList, int mode) {
    //左辺
    ExpressionElement paramX = evxParseExponentiation (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    for (ExpressionElement operator = tokenList.peekFirst ();
         operator != null;
         operator = tokenList.peekFirst ()) {
      ElementType type = operator.exlType;
      if (type == ElementType.ETY_TOKEN_ASTERISK) {  // x*y
        type = ElementType.ETY_OPERATOR_MULTIPLICATION;
      } else if (type == ElementType.ETY_TOKEN_SOLIDUS) {  // x/y
        type = ElementType.ETY_OPERATOR_DIVISION;
      } else if (type == ElementType.ETY_TOKEN_PERCENT_SIGN) {  // x%y
        type = ElementType.ETY_OPERATOR_MODULUS;
      } else {
        break;
      }
      tokenList.pollFirst ();  // *または/または%
      //右辺
      ExpressionElement paramY = evxParseExponentiation (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType == ElementType.ETY_FLOAT &&
          paramY.exlValueType == ElementType.ETY_FLOAT) {  //両辺が浮動小数点数なので普通の演算
        paramX = new ExpressionElement (
          type, 0,
          ElementType.ETY_FLOAT, null, null,
          paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
          paramX, paramY, null);
        continue;
      }
      if (mode == EVM_ASSEMBLER) {  //アセンブラモード
        //  乗算かつ左辺がRnまたはRn.WまたはRn.Lかつ右辺が浮動小数点数ときスケールファクタとみなす
        if (type == ElementType.ETY_OPERATOR_MULTIPLICATION &&  //乗算かつ
            (paramX.exlType == ElementType.ETY_INTEGER_REGISTER ||  // 左辺がRnまたは
             (paramX.exlType == ElementType.ETY_SIZE &&
              paramX.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER)) &&  // Rn.WまたはRn.Lかつ
            paramY.exlValueType == ElementType.ETY_FLOAT) {  //右辺が浮動小数点数
          paramX = new ExpressionElement (
            ElementType.ETY_SCALE_FACTOR, 0,
            ElementType.ETY_UNDEF, null, null,
            paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
            paramX, paramY, null);
          continue;
        }
        if (type == ElementType.ETY_OPERATOR_MULTIPLICATION &&  //乗算かつ
            (paramY.exlType == ElementType.ETY_INTEGER_REGISTER ||  // 右辺がRnまたは
             (paramY.exlType == ElementType.ETY_SIZE &&
              paramY.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER)) &&  // Rn.WまたはRn.Lかつ
            paramX.exlValueType == ElementType.ETY_FLOAT) {  //左辺が浮動小数点数
          paramX = new ExpressionElement (
            ElementType.ETY_SCALE_FACTOR, 0,
            ElementType.ETY_UNDEF, null, null,
            paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
            paramY, paramX, null);
          continue;
        }
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
      } else {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
      }
      return null;
    }  //for operator
    return paramX;
  }  //evxParseMultiplication


  //--------------------------------------------------------------------------------
  //  加減算演算子
  protected ExpressionElement evxParseAddition (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseMultiplication (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement operator = tokenList.peekFirst ();
    while (operator != null) {
      ElementType type = operator.exlType;
      ElementType valueType = paramX.exlValueType;
      ExpressionElement paramY;
      if (type == ElementType.ETY_TOKEN_PLUS_SIGN) {  // x+y
        tokenList.pollFirst ();  // +
        if (mode == EVM_ASSEMBLER &&
            paramX.exlType == ElementType.ETY_REGISTER_INDIRECT &&
            8 <= paramX.exlSubscript) {  // (Ar)+
          return new ExpressionElement (
            ElementType.ETY_POSTINCREMENT, paramX.exlSubscript - 8,
            ElementType.ETY_UNDEF, null, null,
            paramX.exlSource, paramX.exlOffset, operator.exlOffset + operator.exlLength - paramX.exlOffset,
            null, null, null);
        }
        paramY = evxParseMultiplication (tokenList, mode);
        if (paramY == null) {
          return null;
        }
        if (paramX.exlValueType == ElementType.ETY_FLOAT) {
          if (paramY.exlValueType == ElementType.ETY_FLOAT) {  //浮動小数点数+浮動小数点数
            type = ElementType.ETY_OPERATOR_ADDITION_FLOAT_FLOAT;
            valueType = ElementType.ETY_FLOAT;
          } else if (paramY.exlValueType == ElementType.ETY_STRING) {  //浮動小数点数+文字列
            type = ElementType.ETY_OPERATOR_ADDITION_FLOAT_STRING;
            valueType = ElementType.ETY_STRING;
          } else {
            if (mode != EVM_ASSEMBLER) {
              evxPrintError (Multilingual.mlnJapanese ?
                             "2 番目の引数の型が違います" :
                             "wrong type of the 2nd parameter",
                             paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset);
              return null;
            }
          }
        } else if (paramX.exlValueType == ElementType.ETY_STRING) {
          if (paramY.exlValueType == ElementType.ETY_FLOAT) {  //文字列+浮動小数点数
            type = ElementType.ETY_OPERATOR_ADDITION_STRING_FLOAT;
            valueType = ElementType.ETY_STRING;
          } else if (paramY.exlValueType == ElementType.ETY_STRING) {  //文字列+文字列
            type = ElementType.ETY_OPERATOR_ADDITION_STRING_STRING;
            valueType = ElementType.ETY_STRING;
          } else {
            if (mode != EVM_ASSEMBLER) {
              evxPrintError (Multilingual.mlnJapanese ?
                             "2 番目の引数の型が違います" :
                             "wrong type of the 2nd parameter",
                             paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset);
              return null;
            }
          }
        } else {
          if (mode != EVM_ASSEMBLER) {
            evxPrintError (Multilingual.mlnJapanese ?
                           "1 番目の引数の型が違います" :
                           "wrong type of the 1st parameter",
                           paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset);
            return null;
          }
        }
      } else if (mode != EVM_ASSEMBLER &&
                 type == ElementType.ETY_TOKEN_HYPHEN_MINUS) {  // x-y
        tokenList.pollFirst ();  // -
        paramY = evxParseMultiplication (tokenList, mode);
        if (paramY == null) {
          return null;
        }
        if (paramX.exlValueType != ElementType.ETY_FLOAT) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "1 番目の引数の型が違います" :
                         "wrong type of the 1st parameter",
                         paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset);
          return null;
        }
        if (paramY.exlValueType != ElementType.ETY_FLOAT) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "2 番目の引数の型が違います" :
                         "wrong type of the 2nd parameter",
                         paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset);
          return null;
        }
        type = ElementType.ETY_OPERATOR_SUBTRACTION;
        valueType = ElementType.ETY_FLOAT;
      } else {
        return paramX;
      }
      paramX = new ExpressionElement (
        type, 0,
        valueType, null, null,
        paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
        paramX, paramY, null);
      operator = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseAddition


  //--------------------------------------------------------------------------------
  //  シフト演算子
  protected ExpressionElement evxParseShift (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseAddition (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LEFT_SHIFT:  // x<<y
      case ETY_OPERATOR_RIGHT_SHIFT:  // x>>y
      case ETY_OPERATOR_UNSIGNED_RIGHT_SHIFT:  // x>>>y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseAddition (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseShift


  //--------------------------------------------------------------------------------
  //  比較演算子
  protected ExpressionElement evxParseComparison (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseShift (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LESS_THAN:  // x<y
      case ETY_OPERATOR_LESS_OR_EQUAL:  // x<=y
      case ETY_OPERATOR_GREATER_THAN:  // x>y
      case ETY_OPERATOR_GREATER_OR_EQUAL:  // x>=y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseShift (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxComparison


  //--------------------------------------------------------------------------------
  //  等価演算子
  protected ExpressionElement evxParseEquality (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseComparison (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_EQUAL:  // x==y
      case ETY_OPERATOR_NOT_EQUAL:  // x!=y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseComparison (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseEquality


  //--------------------------------------------------------------------------------
  //  ビットAND演算子
  protected ExpressionElement evxParseBitwiseAnd (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseEquality (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_BITWISE_AND:  // x&y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseEquality (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseBitwiseAnd


  //--------------------------------------------------------------------------------
  //  ビットXOR演算子
  protected ExpressionElement evxParseBitwiseXor (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseBitwiseAnd (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_BITWISE_XOR:  // x^y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseBitwiseAnd (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseBitwiseXor


  //--------------------------------------------------------------------------------
  //  ビットOR演算子
  protected ExpressionElement evxParseBitwiseOr (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseBitwiseXor (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_BITWISE_OR:  // x|y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseBitwiseXor (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseBitwiseOr


  //--------------------------------------------------------------------------------
  //  論理AND演算子
  protected ExpressionElement evxParseLogicalAnd (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseBitwiseOr (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LOGICAL_AND:  // x&&y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseBitwiseOr (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseLogicalAnd


  //--------------------------------------------------------------------------------
  //  論理OR演算子
  protected ExpressionElement evxParseLogicalOr (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseLogicalAnd (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    while (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_LOGICAL_OR:  // x||y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseLogicalAnd (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (paramY.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlValueType = ElementType.ETY_FLOAT;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      paramX = elem;
      elem = tokenList.peekFirst ();
    }
    return paramX;
  }  //evxParseLogicalOr


  //--------------------------------------------------------------------------------
  //  条件演算子
  protected ExpressionElement evxParseConditional (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseLogicalOr (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem != null) {
      switch (elem.exlType) {
      case ETY_TOKEN_QUESTION_MARK:  // x?y:z
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseConditional (tokenList, mode);  //右から結合するので自分を呼ぶ
      if (paramY == null) {
        return null;
      }
      ExpressionElement colon = tokenList.pollFirst ();
      if (colon == null) {  //?があるのに:がない
        evxPrintError (Multilingual.mlnJapanese ?
                       ": がありません" :
                       ": is not found",
                       elem.exlSource, -1, 1);
        return null;
      }
      if (colon.exlType != ElementType.ETY_TOKEN_COLON) {  //?があるのに:がない
        evxPrintError (Multilingual.mlnJapanese ?
                       ": がありません" :
                       ": is not found",
                       colon.exlSource, colon.exlOffset, 1);
        return null;
      }
      ExpressionElement paramZ = evxParseConditional (tokenList, mode);  //右から結合するので自分を呼ぶ
      if (paramZ == null) {
        return null;
      }
      if (paramX.exlValueType != ElementType.ETY_FLOAT) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "1 番目の引数の型が違います" :
                       "wrong type of the 1st parameter",
                       paramX.exlSource, paramX.exlOffset, paramZ.exlOffset + paramZ.exlLength - paramX.exlOffset);
        return null;
      }
      if (!(paramY.exlValueType == ElementType.ETY_FLOAT ||
            paramY.exlValueType == ElementType.ETY_STRING)) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "2 番目の引数の型が違います" :
                       "wrong type of the 2nd parameter",
                       paramX.exlSource, paramX.exlOffset, paramZ.exlOffset + paramZ.exlLength - paramX.exlOffset);
        return null;
      } else if (paramY.exlValueType != paramZ.exlValueType) {
        evxPrintError (Multilingual.mlnJapanese ?
                       "3 番目の引数の型が違います" :
                       "wrong type of the 3rd parameter",
                       paramX.exlSource, paramX.exlOffset, paramZ.exlOffset + paramZ.exlLength - paramX.exlOffset);
        return null;
      }
      elem.exlType = paramY.exlValueType == ElementType.ETY_FLOAT ? ElementType.ETY_OPERATOR_CONDITIONAL_FLOAT : ElementType.ETY_OPERATOR_CONDITIONAL_STRING;
      elem.exlValueType = paramY.exlValueType;
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlParamZ = paramZ;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramZ.exlOffset + paramZ.exlLength - paramX.exlOffset;
      return elem;
    }
    return paramX;
  }  //evxParseConditional


  //--------------------------------------------------------------------------------
  //  代入演算子
  protected ExpressionElement evxParseAssignment (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = evxParseConditional (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem != null) {
      switch (elem.exlType) {
      case ETY_OPERATOR_ASSIGNMENT:  // x=y
      case ETY_OPERATOR_SELF_POWER:  // x**=y
      case ETY_OPERATOR_SELF_MULTIPLICATION:  // x*=y
      case ETY_OPERATOR_SELF_DIVISION:  // x/=y
      case ETY_OPERATOR_SELF_MODULUS:  // x%=y
      case ETY_OPERATOR_SELF_ADDITION:  // x+=y
      case ETY_OPERATOR_SELF_SUBTRACTION:  // x-=y
      case ETY_OPERATOR_SELF_LEFT_SHIFT:  // x<<=y
      case ETY_OPERATOR_SELF_RIGHT_SHIFT:  // x>>=y
      case ETY_OPERATOR_SELF_UNSIGNED_RIGHT_SHIFT:  // x>>>=y
      case ETY_OPERATOR_SELF_BITWISE_AND:  // x&=y
      case ETY_OPERATOR_SELF_BITWISE_XOR:  // x^=y
      case ETY_OPERATOR_SELF_BITWISE_OR:  // x|=y
        break;
      default:
        return paramX;
      }
      tokenList.pollFirst ();
      ExpressionElement paramY = evxParseAssignment (tokenList, mode);  //右から結合するので自分を呼ぶ
      if (paramY == null) {
        return null;
      }
      if (mode == EVM_ASSEMBLER) {  //アセンブラモード
        evxPrintError (Multilingual.mlnJapanese ?
                       "副作用を起こす演算子はここでは使えません" :
                       "operators which cause a side effect is unusable here",
                       elem.exlSource, elem.exlOffset, elem.exlLength);
        return null;
      }
      if (!(paramX.exlIsFloatSubstituend () ||  //数値被代入項
            paramX.exlType == ElementType.ETY_VARIABLE_STRING)) {  // 文字列変数
        evxPrintError (Multilingual.mlnJapanese ?
                       "引数が場所を示していません" :
                       "parameter is not indicating a location",
                       paramX.exlSource, paramX.exlOffset, paramX.exlLength);
        return null;
      }
      if (elem.exlType == ElementType.ETY_OPERATOR_ASSIGNMENT &&  //単純代入
          paramX.exlType == ElementType.ETY_VARIABLE_STRING &&  //左辺が文字列変数
          paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_ASSIGN_STRING_TO_VARIABLE;  //文字列変数への文字列単純代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (elem.exlType == ElementType.ETY_OPERATOR_SELF_ADDITION &&  //加算・連結複合代入
                 paramX.exlType == ElementType.ETY_VARIABLE_STRING &&  //左辺が文字列変数
                 paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_CONCAT_STRING_TO_VARIABLE;  //文字列変数への文字列連結複合代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (elem.exlType == ElementType.ETY_OPERATOR_ASSIGNMENT &&  //単純代入
                 paramX.exlType == ElementType.ETY_SQUARE_BRACKET &&  //左辺がメモリ
                 paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_ASSIGN_STRING_TO_MEMORY;  //メモリへの文字列単純代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (elem.exlType == ElementType.ETY_OPERATOR_SELF_ADDITION &&  //加算・連結複合代入
                 paramX.exlType == ElementType.ETY_SQUARE_BRACKET &&  //左辺がメモリ
                 paramY.exlValueType == ElementType.ETY_STRING) {  //右辺が文字列
        elem.exlType = ElementType.ETY_OPERATOR_CONCAT_STRING_TO_MEMORY;  //メモリへの文字列連結複合代入に変更
        elem.exlValueType = ElementType.ETY_STRING;
      } else if (paramX.exlType != ElementType.ETY_VARIABLE_STRING &&  //左辺が文字列変数ではない
                 paramY.exlValueType == ElementType.ETY_FLOAT) {  //右辺が数値
        elem.exlValueType = ElementType.ETY_FLOAT;
      } else {
        evxPrintError (Multilingual.mlnJapanese ?
                       "引数の型が違います" :
                       "wrong type of parameter",
                       paramY.exlSource, paramY.exlOffset, paramY.exlLength);
        return null;
      }
      elem.exlParamX = paramX;
      elem.exlParamY = paramY;
      elem.exlOffset = paramX.exlOffset;
      elem.exlLength = paramY.exlOffset + paramY.exlLength - elem.exlOffset;
      return elem;
    }
    return paramX;
  }  //evxParseAssignment


  //--------------------------------------------------------------------------------
  //  コロン演算子
  protected ExpressionElement evxParseColon (LinkedList<ExpressionElement> tokenList, int mode) {
    //左辺
    ExpressionElement paramX = evxParseAssignment (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    for (ExpressionElement operator = tokenList.peekFirst ();
         operator != null && operator.exlType == ElementType.ETY_TOKEN_COLON;  // :
         operator = tokenList.peekFirst ()) {
      tokenList.pollFirst ();  // :
      //右辺
      ExpressionElement paramY = evxParseAssignment (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      if (mode == EVM_ASSEMBLER) {  //アセンブラモード
        if (paramX.exlType == ElementType.ETY_INTEGER_REGISTER && paramX.exlSubscript < 8 &&  //Dh
            paramY.exlType == ElementType.ETY_INTEGER_REGISTER && paramY.exlSubscript < 8) {  //Dl
          //データレジスタペア
          int h = paramX.exlSubscript;
          int l = paramY.exlSubscript;
          int subscript = h << 3 | l;
          return new ExpressionElement (
            ElementType.ETY_DATA_REGISTER_PAIR, subscript,
            ElementType.ETY_UNDEF, null, null,
            paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
            null, null, null);
        }
        if (paramX.exlType == ElementType.ETY_REGISTER_INDIRECT &&  //(Rr)
            paramY.exlType == ElementType.ETY_REGISTER_INDIRECT) {  //(Rs)
          //レジスタ間接ペア
          int r = paramX.exlSubscript;
          int s = paramY.exlSubscript;
          int subscript = r << 4 | s;
          return new ExpressionElement (
            ElementType.ETY_REGISTER_INDIRECT_PAIR, subscript,
            ElementType.ETY_UNDEF, null, null,
            paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
            null, null, null);
        }
      }
      paramX = new ExpressionElement (
        ElementType.ETY_OPERATOR_COLON, 0,
        ElementType.ETY_UNDEF, null, null,
        paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
        paramX, paramY, null);
    }
    return paramX;
  }  //evxParseColon


  //--------------------------------------------------------------------------------
  //  コンマ演算子
  protected ExpressionElement evxParseComma (LinkedList<ExpressionElement> tokenList, int mode) {
    //左辺
    ExpressionElement paramX = evxParseColon (tokenList, mode);
    if (paramX == null) {
      return null;
    }
    for (ExpressionElement operator = tokenList.peekFirst ();
         operator != null && operator.exlType == ElementType.ETY_TOKEN_COMMA;  // ,
         operator = tokenList.peekFirst ()) {
      tokenList.pollFirst ();  // ,
      //右辺
      ExpressionElement paramY = evxParseColon (tokenList, mode);
      if (paramY == null) {
        return null;
      }
      paramX = new ExpressionElement (
        ElementType.ETY_OPERATOR_COMMA, 0,
        paramY.exlValueType, null, null,
        paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset,
        paramX, paramY, null);
    }
    return paramX;
  }  //evxParseComma


  //--------------------------------------------------------------------------------
  //  コマンド
  protected ExpressionElement evxParseCommand (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem == null) {
      return null;
    }
    int minParamCount = 0;
    int maxParamCount = 0;
    switch (elem.exlType) {
    case ETY_COMMAND_RUN:  //g
    case ETY_COMMAND_STEP:  //s
    case ETY_COMMAND_TRACE:  //t
    case ETY_COMMAND_TRACE_REGS:  //tx
    case ETY_COMMAND_TRACE_FLOAT_REGS:  //txf
      maxParamCount = 1;
      break;
    case ETY_COMMAND_DUMP:  //d
    case ETY_COMMAND_LIST:  //l
      maxParamCount = 2;
      break;
    case ETY_COMMAND_MEMORY_EDIT:  //me
      minParamCount = 1 + 1;
      maxParamCount = 1 + 65536;
      break;
    case ETY_COMMAND_FILL:  //f
    case ETY_COMMAND_MEMORY_SEARCH:  //ms
      minParamCount = 2 + 1;
      maxParamCount = 2 + 65536;
      break;
    case ETY_COMMAND_HELP:  //h
      maxParamCount = 65536;  //ヘルプの引数は何を書いてもよいがすべて無視する
      break;
    case ETY_COMMAND_PRINT:  //p
      maxParamCount = 65536;
      break;
    case ETY_COMMAND_STOP:  //i
    case ETY_COMMAND_LABEL_LIST:  //ll
    case ETY_COMMAND_RETURN:  //r
    case ETY_COMMAND_REGS:  //x
    case ETY_COMMAND_FLOAT_REGS:  //xf
      break;
    default:
      return evxParseComma (tokenList, mode);
    }
    elem = tokenList.pollFirst ();
    int paramCount = 0;
    ExpressionElement paramX = null;
    if (!tokenList.isEmpty () &&
        tokenList.peekFirst ().exlType != ElementType.ETY_TOKEN_SEMICOLON) {  //引数がある
      paramX = evxParseComma (tokenList, mode);
      if (paramX == null) {
        return null;
      }
      paramCount = paramX.exlLengthOfCommaList ();  //引数リストの長さ
    }
    if (paramCount < minParamCount) {
      evxPrintError (Multilingual.mlnJapanese ?
                     "引数が足りません" :
                     "too few arguments",
                     elem.exlSource, elem.exlOffset, elem.exlLength);
      return null;
    }
    if (maxParamCount < paramCount) {
      evxPrintError (Multilingual.mlnJapanese ?
                     "引数が多すぎます" :
                     "too many arguments",
                     elem.exlSource, elem.exlOffset, elem.exlLength);
      return null;
    }
    elem.exlValueType = ElementType.ETY_VOID;
    elem.exlParamX = paramX;
    return elem;
  }  //evxParseCommand


  //--------------------------------------------------------------------------------
  //  ライン
  protected ExpressionElement evxParseLine (LinkedList<ExpressionElement> tokenList, int mode) {
    ExpressionElement paramX = null;  //ラベル
    String stringValue = null;  //ニモニック
    int subscript = -1;  //オペレーションサイズ
    ExpressionElement paramY = null;  //オペランド
    ExpressionElement elem = tokenList.peekFirst ();
    if (elem == null) {
      return null;
    }
    ExpressionElement head = elem;
    ExpressionElement tail = elem;
    //ラベル
    if (elem.exlType == ElementType.ETY_LABEL_DEFINITION ||
        elem.exlType == ElementType.ETY_LOCAL_LABEL_DEFINITION) {
      paramX = elem;
      tokenList.pollFirst ();
      elem = tokenList.peekFirst ();
    }
    //ニモニック
    elem = tokenList.peekFirst ();
    if (elem != null && elem.exlType == ElementType.ETY_MNEMONIC) {
      tail = elem;
      stringValue = elem.exlStringValue;
      tokenList.pollFirst ();
      elem = tokenList.peekFirst ();
      //オペレーションサイズ
      if (elem != null && (elem.exlType == ElementType.ETY_OPERATOR_SIZE_BYTE ||  // x.b
                           elem.exlType == ElementType.ETY_OPERATOR_SIZE_WORD ||  // x.w
                           elem.exlType == ElementType.ETY_OPERATOR_SIZE_LONG ||  // x.l
                           elem.exlType == ElementType.ETY_OPERATOR_SIZE_QUAD ||  // x.q
                           elem.exlType == ElementType.ETY_OPERATOR_SIZE_SINGLE ||  // x.s
                           elem.exlType == ElementType.ETY_OPERATOR_SIZE_DOUBLE ||  // x.d
                           elem.exlType == ElementType.ETY_OPERATOR_SIZE_EXTENDED ||  // x.x
                           elem.exlType == ElementType.ETY_OPERATOR_SIZE_PACKED)) {  // x.p
        tail = elem;
        subscript = elem.exlSubscript;
        tokenList.pollFirst ();
        elem = tokenList.peekFirst ();
      }
      //オペランド
      if (elem != null && elem.exlType != ElementType.ETY_TOKEN_SEMICOLON) {
        paramY = evxParseComma (tokenList, mode);
        tail = paramY;
        elem = tokenList.peekFirst ();
      }
    }
    if (elem != null && elem.exlType != ElementType.ETY_TOKEN_SEMICOLON) {
      evxPrintError (Multilingual.mlnJapanese ?
                     "文法エラー" :
                     "syntax error",
                     elem.exlSource, elem.exlOffset, elem.exlLength);
      return null;
    }
    return new ExpressionElement (
      ElementType.ETY_LINE, subscript,
      ElementType.ETY_VOID, null, stringValue,
      head.exlSource, head.exlOffset, tail.exlOffset + tail.exlLength - head.exlOffset,
      paramX, paramY, null);
  }  //evxParseLine


  //--------------------------------------------------------------------------------
  //  セパレータ
  protected ExpressionElement evxParseSeparator (LinkedList<ExpressionElement> tokenList, int mode) {
    //左辺
    ExpressionElement paramX = (mode == EVM_EXPRESSION ? evxParseComma (tokenList, mode) :  //式評価モード
                                mode == EVM_COMMAND ? evxParseCommand (tokenList, mode) :  //コマンドモード
                                mode == EVM_ASSEMBLER ? evxParseLine (tokenList, mode) :  //アセンブラモード
                                null);
    if (paramX == null) {
      return null;
    }
    for (ExpressionElement elem = tokenList.peekFirst ();
         elem != null &&
         elem.exlType == ElementType.ETY_TOKEN_SEMICOLON;  // ;
         elem = tokenList.peekFirst ()) {
      //;の並びを読み飛ばす
      do {
        tokenList.pollFirst ();
        if (tokenList.isEmpty ()) {  //;の並びで終わった
          break;
        }
        elem = tokenList.peekFirst ();
      } while (elem.exlType == ElementType.ETY_TOKEN_SEMICOLON);
      //右辺
      ExpressionElement paramY = (mode == EVM_EXPRESSION ? evxParseComma (tokenList, mode) :  //式評価モード
                                  mode == EVM_COMMAND ? evxParseCommand (tokenList, mode) :  //コマンドモード
                                  mode == EVM_ASSEMBLER ? evxParseLine (tokenList, mode) :  //アセンブラモード
                                  null);
      if (paramY == null) {
        return null;
      }
      ExpressionElement separator = new ExpressionElement (
        ElementType.ETY_SEPARATOR, 0,
        paramY.exlValueType, null, null,
        paramX.exlSource, paramX.exlOffset, paramY.exlOffset + paramY.exlLength - paramX.exlOffset);
      separator.exlParamX = paramX;
      separator.exlParamY = paramY;
      paramX = separator;
    }
    return paramX;
  }  //evxParseSeparator



  //オペランドモード
  //  コンストラクタを呼び出すときに指定する
  public static final int ORM_BASIC     = 0;  //基本
  public static final int ORM_BIT_FIELD = 1;  //ビットフィールド付き
  public static final int ORM_K_FACTOR  = 2;  //k-factor付き
  public static final int ORM_RELATIVE  = 3;  //相対アドレス


  //オペランド型
  //  コンストラクタがオペランドを見て決める
  //  原則としてサイズの最適化でオペランド型と添字が変化することはない
  //  JBcc→BNcc+JMPは命令側で処理する
  public static final int ORT_ERROR            = -1;  //                  エラー
  public static final int ORT_DATA_REGISTER    =  0;  // Dr               データレジスタ直接。0..7
  public static final int ORT_ADDRESS_REGISTER =  1;  // Ar               アドレスレジスタ直接。0..7
  public static final int ORT_POSTINCREMENT    =  2;  // (Ar)+            アドレスレジスタ間接ポストインクリメント付き。0..7
  public static final int ORT_PREDECREMENT     =  3;  // -(Ar)            アドレスレジスタ間接プリデクリメント付き。0..7
  public static final int ORT_IMMEDIATE        =  4;  // #<data>          イミディエイト
  public static final int ORT_ABSOLUTE_ADDRESS =  5;  // xxx              絶対アドレス。絶対分岐命令の数値オペランド
  //                                                     (xxx).W          絶対ショート
  //                                                     (xxx).L          絶対ロング
  public static final int ORT_RELATIVE_ADDRESS =  6;  // xxx              相対アドレス。相対分岐命令の数値オペランド
  public static final int ORT_ADDRESS_INDIRECT =  7;  // (Ar)             アドレスレジスタ間接。0..7
  //                                                     (d16,Ar)         アドレスレジスタ間接ディスプレースメント付き。0..7
  //                                                     (d8,Ar,Rn.wl)    アドレスレジスタ間接インデックス付き。0..7
  public static final int ORT_PROGRAM_INDIRECT =  8;  //                  プログラムカウンタ間接
  //                                                     (d16,PC)         プログラムカウンタ間接ディスプレースメント付き
  //                                                     (d8,PC,Rn.wl)    プログラムカウンタ間接インデックス付き
  public static final int ORT_CONDITION_CODE   =  9;  // CCR              コンディションコードレジスタ
  public static final int ORT_STATUS_REGISTER  = 10;  // SR               ステータスレジスタ
  public static final int ORT_REGISTER_LIST    = 11;  // D0-D7/A0-A7      レジスタリスト。0x0000..0xffff
  public static final int ORT_REGISTER_PAIR    = 12;  // Dh:Dl            データレジスタペア。0x00..0x3f
  public static final int ORT_INDIRECT_PAIR    = 13;  // (Rr):(Rs)        レジスタ間接ペア。0x00..0xff
  public static final int ORT_FLOAT_REGISTER   = 14;  // FPn              浮動小数点レジスタ。0..7
  public static final int ORT_FLOAT_LIST       = 15;  // FP0-FP7          浮動小数点レジスタリスト。0x00..0xff
  public static final int ORT_FLOAT_CONTROL    = 16;  // FPIAR/FPSR/FPCR  浮動小数点制御レジスタ(リスト)。0..7
  public static final int ORT_CACHE_SELECTION  = 17;  // NC DC IC BC      キャッシュ選択。0..3
  public static final int ORT_CONTROL_REGISTER = 18;  // SFC ...          制御レジスタ(MOVEC)。0x0000..0xffff
  //public static final int ORT_MMU_REGISTER     = 19;  // TT0 ...          メモリ管理レジスタ(PMOVE)。0x0000..0xffff


  //class Operand
  //  オペランド
  public class Operand {

    //オペランドモード
    public int asoOperandMode;  //オペランドモード

    //オペランド
    public ExpressionElement asoOperandElement;  //オペランドの要素
    public ExpressionElement asoBasicElement;  //ビットフィールドまたはk-factorを除いた基本要素

    //オペレーションサイズ
    public int asoMinimumOperationSize;  //最小オペレーションサイズ。1,2,4
    public int asoMaximumOperationSize;  //最大オペレーションサイズ。1,2,4
    public int asoOperationSize;  //オペレーションサイズ。1,2,4

    //ビットフィールド
    //  ビットフィールド付きデータレジスタ
    //  ビットフィールド付きメモリ実効アドレス
    public boolean asoWithBitField;  //true=ビットフィールド付き
    //  以下はasoWithBitField==trueのとき有効
    public int asoBitFieldOffsetRegister;  //オフセットのレジスタ番号。-1=イミディエイト
    public ExpressionElement asoBitFieldOffsetElement;  //オフセットの要素
    public int asoBitFieldOffsetValue;  //オフセットの値
    public int asoBitFieldWidthRegister;  //幅のレジスタ番号。-1=イミディエイト
    public ExpressionElement asoBitFieldWidthElement;  //幅の要素
    public int asoBitFieldWidthValue;  //幅の値

    //k-factor
    //  k-factor付きメモリ実効アドレス
    public boolean asoWithKFactor;  //true=k-factor付き
    //  以下はasoWithKFactor==trueのとき有効
    public int asoKFactorRegister;  //k-factorのレジスタ番号。-1=イミディエイト
    public ExpressionElement asoKFactorElement;  //k-factorの式
    public int asoKFactorValue;  //k-factorの式の値

    //オペランド型
    public int asoOperandType;  //オペランド型
    public int asoSubscript;  //添字。レジスタ番号など

    //実効アドレス
    //  ベースレジスタ
    public boolean asoBaseRegisterSuppress;  //ベースレジスタサプレス
    //  ベースディスプレースメント
    public int asoMinimumBaseDisplacementSize;  //最小ベースディスプレースメントサイズ。0,1,2,4
    public int asoMaximumBaseDisplacementSize;  //最大ベースディスプレースメントサイズ。0,1,2,4
    public ExpressionElement asoBaseDisplacementElement;  //ベースディスプレースメントの要素
    public int asoBaseDisplacementValue;  //ベースディスプレースメントの値
    public int asoBaseDisplacementSize;  //ベースディスプレースメントサイズ。0,1,2,4
    //  メモリ間接
    public boolean asoMemoryIndirect;  //メモリ間接
    //  アウタディスプレースメント
    //    メモリ間接のとき有効
    public int asoMinimumOuterDisplacementSize;  //最小アウタディスプレースメントサイズ。0,2,4
    public int asoMaximumOuterDisplacementSize;  //最大アウタディスプレースメントサイズ。0,2,4
    public ExpressionElement asoOuterDisplacementElement;  //アウタディスプレースメントの要素
    public int asoOuterDisplacementValue;  //アウタディスプレースメントの値
    public int asoOuterDisplacementSize;  //アウタディスプレースメントサイズ。0,2,4
    //  インデックス
    public boolean asoPostindex;  //ポストインデックス
    public boolean asoIndexSuppress;  //インデックスサプレス
    public int asoIndexRegister;  //インデックスレジスタ。0..15
    public ExpressionElement asoScaleFactorElement;  //スケールファクタの要素
    public int asoScaleFactorValue;  //スケールファクタの値
    public int asoLog2ScaleFactor;  //スケールファクタのlog2。0..3
    //  イミディエイト
    //    最終パスでイミディエイトの値のサイズがオペレーションサイズを超えていたらエラー
    public ExpressionElement asoImmediateElement;  //イミディエイトの式
    public int asoImmediateValue;  //イミディエイトの式の値
    public int asoImmediateSize;  //イミディエイトの式の値のサイズ
    //  絶対アドレス
    //    サイズ付きのとき最終パスで絶対アドレスの値のサイズが指定されたサイズを超えていたらエラー
    public int asoMinimumAbsoluteAddressSize;  //最小絶対アドレスサイズ
    public int asoMaximumAbsoluteAddressSize;  //最大絶対アドレスサイズ
    public ExpressionElement asoAbsoluteAddressElement;  //絶対アドレスの式
    public int asoAbsoluteAddressValue;  //絶対アドレスの式の値
    public int asoAbsoluteAddressSize;  //絶対アドレスの式の値のサイズ

    //相対アドレス
    //  asoOperandMode==ORM_RELATIVEのとき有効
    public int asoBaseAddress;  //ベースアドレス。最初の拡張ワードの位置。通常は命令の先頭+2。FBccのとき命令の先頭+4。ディスプレースメントの位置ではない
    public ExpressionElement asoRelativeAddressElement;  //相対アドレスの式(絶対アドレス)
    public int asoRelativeAddressValue;  //相対アドレスの式の値-ベースアドレス(相対アドレス)
    public int asoRelativeAddressSize;  //相対アドレスの式の値-ベースアドレス(相対アドレス)のサイズ。0,1,2,4


    //----------------------------------------------------------------
    //  コンストラクタ
    public Operand (int operandMode, ExpressionElement operandElement,
                    int minimumOperationSize, int maximumOperationSize) {
      this (operandMode, operandElement,
            minimumOperationSize, maximumOperationSize,
            0);
    }
    public Operand (int operandMode, ExpressionElement operandElement,
                    int minimumOperationSize, int maximumOperationSize,
                    int baseAddress) {
      asoOperandMode = operandMode;
      asoOperandElement = operandElement;
      asoMinimumOperationSize = minimumOperationSize;
      asoMaximumOperationSize = maximumOperationSize;
      asoBaseAddress = baseAddress;

      asoBasicElement = asoOperandElement;
      asoOperationSize = maximumOperationSize;

      //------------------------------------------------
      //ビットフィールドを分離する
      //    <ea>{<offset>:<width>}
      asoWithBitField = false;
      if (asoOperandMode != ORM_BIT_FIELD) {
        if (asoOperandElement.exlType == ElementType.ETY_BIT_FIELD) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "予期しないビットフィールド" :
                         "unexpected bit-field",
                         asoOperandElement.exlSource, asoOperandElement.exlOffset, asoOperandElement.exlLength);
          asoOperandType = ORT_ERROR;
          return;
        }
      } else {
        if (asoOperandElement.exlType != ElementType.ETY_BIT_FIELD) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "ビットフィールドが必要です" :
                         "bit-field is required",
                         asoOperandElement.exlSource, asoOperandElement.exlOffset, asoOperandElement.exlLength);
          asoOperandType = ORT_ERROR;
          return;
        }
        asoBasicElement = asoOperandElement.exlParamX;  // <ea>
        asoBitFieldOffsetRegister = -1;
        asoBitFieldOffsetElement = asoOperandElement.exlParamY;  // oまたは#oまたはDo
        if (asoBitFieldOffsetElement.exlType == ElementType.ETY_INTEGER_REGISTER) {  // Do
          asoBitFieldOffsetRegister = asoBitFieldOffsetElement.exlSubscript;
        } else {  // oまたは#o
          if (asoBitFieldOffsetElement.exlType == ElementType.ETY_IMMEDIATE) {  // #o
            asoBitFieldOffsetElement = asoBitFieldOffsetElement.exlParamX;  // o
          }
          asoBitFieldOffsetValue = asoBitFieldOffsetElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
        }
        asoBitFieldWidthRegister = -1;
        asoBitFieldWidthElement = asoOperandElement.exlParamZ;  // wまたは#wまたはDw
        if (asoBitFieldWidthElement.exlType == ElementType.ETY_INTEGER_REGISTER) {  // Dw
          asoBitFieldWidthRegister = asoBitFieldWidthElement.exlSubscript;
        } else {  //wまたは#w
          if (asoBitFieldWidthElement.exlType == ElementType.ETY_IMMEDIATE) {  // #w
            asoBitFieldWidthElement = asoBitFieldWidthElement.exlParamX;  // w
          }
          asoBitFieldWidthValue = asoBitFieldWidthElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
        }
      }

      //------------------------------------------------
      //k-factorを分離する
      //    <ea>{<k-factor>}
      asoWithKFactor = false;
      if (asoOperandMode != ORM_K_FACTOR) {
        if (asoOperandElement.exlType == ElementType.ETY_K_FACTOR) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "予期しない k-factor" :
                         "unexpected k-factor",
                         asoOperandElement.exlSource, asoOperandElement.exlOffset, asoOperandElement.exlLength);
          asoOperandType = ORT_ERROR;
          return;
        }
      } else {
        if (asoOperandElement.exlType != ElementType.ETY_K_FACTOR) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "k-factor が必要です" :
                         "k-factor is required",
                         asoOperandElement.exlSource, asoOperandElement.exlOffset, asoOperandElement.exlLength);
          asoOperandType = ORT_ERROR;
          return;
        }
        asoBasicElement = asoOperandElement.exlParamX;  // <ea>
        asoKFactorRegister = -1;
        asoKFactorElement = asoOperandElement.exlParamY;  // kまたは#kまたはDk
        if (asoKFactorElement.exlType == ElementType.ETY_INTEGER_REGISTER) {  // Dk
          asoKFactorRegister = asoKFactorElement.exlSubscript;
        } else {  //kまたは#k
          if (asoKFactorElement.exlType == ElementType.ETY_IMMEDIATE) {  // #k
            asoKFactorElement = asoKFactorElement.exlParamX;  // k
          }
          asoKFactorValue = asoKFactorElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
        }
      }

      //実効アドレス
      //  (Ar)で初期化する
      //  ベースレジスタ
      asoBaseRegisterSuppress = false;
      //  ベースディスプレースメント
      asoMinimumBaseDisplacementSize = 0;
      asoMaximumBaseDisplacementSize = 0;
      asoBaseDisplacementElement = null;
      asoBaseDisplacementValue = 0;
      asoBaseDisplacementSize = 0;
      //  メモリ間接
      asoMemoryIndirect = false;
      //  アウタディスプレースメント
      asoMinimumOuterDisplacementSize = 0;
      asoMaximumOuterDisplacementSize = 0;
      asoOuterDisplacementElement = null;
      asoOuterDisplacementValue = 0;
      asoOuterDisplacementSize = 0;
      //  インデックス
      asoPostindex = false;
      asoIndexSuppress = true;
      asoIndexRegister = 0;
      asoScaleFactorElement = null;
      asoScaleFactorValue = 1;
      asoLog2ScaleFactor = 0;  // ZD0.W*1
      //  イミディエイト
      asoImmediateElement = null;
      asoImmediateValue = 0;
      asoImmediateSize = 0;
      //  絶対アドレス
      asoMinimumAbsoluteAddressSize = 2;
      asoMaximumAbsoluteAddressSize = 4;
      asoAbsoluteAddressElement = null;
      asoAbsoluteAddressValue = 0;
      asoAbsoluteAddressSize = 0;

      //------------------------------------------------
      //相対アドレス
      if (asoOperandMode == ORM_RELATIVE) {
        if (asoBasicElement.exlValueType != ElementType.ETY_FLOAT) {
          evxPrintError (Multilingual.mlnJapanese ?
                         "相対アドレスが必要です" :
                         "relative address is required",
                         asoBasicElement.exlSource, asoBasicElement.exlOffset, asoBasicElement.exlLength);
          asoOperandType = ORT_ERROR;
          return;
        }
        asoOperandType = ORT_RELATIVE_ADDRESS;
        asoRelativeAddressElement = asoBasicElement;
        asoRelativeAddressValue = asoRelativeAddressElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti () - asoBaseAddress;
        asoRelativeAddressSize = (asoRelativeAddressValue == 0 ? 0 :  //BRA/BSR/Bccは0を2にする
                                  (byte) asoRelativeAddressValue == asoRelativeAddressValue ? 1 :
                                  (short) asoRelativeAddressValue == asoRelativeAddressValue ? 2 :
                                  4);
        return;
      }

      //------------------------------------------------
      //  Dr               データレジスタ直接。0..7
      //  Ar               アドレスレジスタ直接。0..7
      if (asoBasicElement.exlType == ElementType.ETY_INTEGER_REGISTER) {
        if (asoBasicElement.exlSubscript < 8) {
          asoOperandType = ORT_DATA_REGISTER;
          asoSubscript = asoBasicElement.exlSubscript;
        } else {
          asoOperandType = ORT_ADDRESS_REGISTER;
          asoSubscript = asoBasicElement.exlSubscript - 8;
        }
        return;
      }

      //------------------------------------------------
      //  (Ar)+            アドレスレジスタ間接ポストインクリメント付き。0..7
      if (asoBasicElement.exlType == ElementType.ETY_POSTINCREMENT) {
        asoOperandType = ORT_POSTINCREMENT;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  -(Ar)            アドレスレジスタ間接プリデクリメント付き。0..7
      if (asoBasicElement.exlType == ElementType.ETY_PREDECREMENT) {
        asoOperandType = ORT_PREDECREMENT;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  #<data>          イミディエイト
      if (asoBasicElement.exlType == ElementType.ETY_IMMEDIATE) {
        asoOperandType = ORT_IMMEDIATE;
        asoImmediateElement = asoBasicElement.exlParamX;
        asoImmediateValue = asoImmediateElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
        asoImmediateSize = (asoImmediateValue == 0 ? 0 :
                            (byte) asoImmediateValue == asoImmediateValue ? 1 :
                            (short) asoImmediateValue == asoImmediateValue ? 2 :
                            4);
        return;
      }

      //------------------------------------------------
      //  xxx              絶対アドレス。絶対分岐命令の数値オペランド
      if (asoBasicElement.exlValueType == ElementType.ETY_FLOAT) {
        asoOperandType = ORT_ABSOLUTE_ADDRESS;
        asoMinimumAbsoluteAddressSize = 2;
        asoMaximumAbsoluteAddressSize = 4;
        asoAbsoluteAddressElement = asoBasicElement;
        asoAbsoluteAddressValue = asoAbsoluteAddressElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
        asoAbsoluteAddressSize = ((short) asoAbsoluteAddressValue == asoAbsoluteAddressValue ? 2 :
                                  4);
        return;
      }

      //------------------------------------------------
      //  (xxx).W          絶対ショート
      //  (xxx).L          絶対ロング
      if (asoBasicElement.exlType == ElementType.ETY_SIZE && (asoBasicElement.exlSubscript == 'w' ||
                                                              asoBasicElement.exlSubscript == 'l') &&
          asoBasicElement.exlParamX.exlValueType == ElementType.ETY_FLOAT) {
        asoOperandType = ORT_ABSOLUTE_ADDRESS;
        if (asoBasicElement.exlSubscript == 'w') {  // (xxx).W
          asoMinimumAbsoluteAddressSize = 2;
          asoMaximumAbsoluteAddressSize = 2;
        } else {  // (xxx).L
          asoMinimumAbsoluteAddressSize = 4;
          asoMaximumAbsoluteAddressSize = 4;
        }
        asoAbsoluteAddressElement = asoBasicElement.exlParamX;
        asoAbsoluteAddressValue = asoAbsoluteAddressElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
        asoAbsoluteAddressSize = ((short) asoAbsoluteAddressValue == asoAbsoluteAddressValue ? 2 :
                                  4);
        return;
      }

      //------------------------------------------------
      //  (Ar)             アドレスレジスタ間接
      if (asoBasicElement.exlType == ElementType.ETY_REGISTER_INDIRECT &&
          8 <= asoBasicElement.exlSubscript) {  // (Ar)
        asoOperandType = ORT_ADDRESS_INDIRECT;
        asoSubscript = asoBasicElement.exlSubscript - 8;
        return;
      }

      //------------------------------------------------
      //  CCR              コンディションコードレジスタ
      if (asoBasicElement.exlType == ElementType.ETY_CCR) {
        asoOperandType = ORT_CONDITION_CODE;
        return;
      }

      //------------------------------------------------
      //  SR               ステータスレジスタ
      if (asoBasicElement.exlType == ElementType.ETY_SR) {
        asoOperandType = ORT_STATUS_REGISTER;
        return;
      }

      //------------------------------------------------
      //  D0-D7/A0-A7      レジスタリスト。0x0000..0xffff
      if (asoBasicElement.exlType == ElementType.ETY_INTEGER_REGISTER_LIST) {
        asoOperandType = ORT_REGISTER_LIST;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  Dh:Dl            データレジスタペア。0x00..0x3f
      if (asoBasicElement.exlType == ElementType.ETY_DATA_REGISTER_PAIR) {
        asoOperandType = ORT_REGISTER_PAIR;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  (Rr):(Rs)        レジスタ間接ペア。0x00..0xff
      if (asoBasicElement.exlType == ElementType.ETY_REGISTER_INDIRECT_PAIR) {
        asoOperandType = ORT_INDIRECT_PAIR;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  FPn              浮動小数点レジスタ。0..7
      if (asoBasicElement.exlType == ElementType.ETY_FLOATING_POINT_REGISTER) {
        asoOperandType = ORT_FLOAT_REGISTER;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  FP0-FP7          浮動小数点レジスタリスト。0x00..0xff
      if (asoBasicElement.exlType == ElementType.ETY_FLOATING_POINT_REGISTER_LIST) {
        asoOperandType = ORT_FLOAT_LIST;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  FPIAR/FPSR/FPCR  浮動小数点制御レジスタ(リスト)。0..7
      if (asoBasicElement.exlType == ElementType.ETY_FLOATING_POINT_CONTROL_REGISTER_LIST) {
        asoOperandType = ORT_FLOAT_CONTROL;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  NC DC IC BC      キャッシュ選択。0..3
      if (asoBasicElement.exlType == ElementType.ETY_CACHE_SELECTION) {
        asoOperandType = ORT_CACHE_SELECTION;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  SFC ...          制御レジスタ(MOVEC)。0x0000..0xffff
      if (asoBasicElement.exlType == ElementType.ETY_CONTROL_REGISTER) {
        asoOperandType = ORT_CONTROL_REGISTER;
        asoSubscript = asoBasicElement.exlSubscript;
        return;
      }

      //------------------------------------------------
      //  TT0 ...          メモリ管理レジスタ(PMOVE)。0x0000..0xffff
      //if (asoBasicElement.exlType == ElementType.ETY_MMU_REGISTER) {
      //  asoOperandType = ORT_MMU_REGISTER;
      //  asoSubscript = asoBasicElement.exlSubscript;
      //  return;
      //}

      //------------------------------------------------
      //  (d16,Ar)         アドレスレジスタ間接ディスプレースメント付き
      //  (d8,Ar,Rn.wl)    アドレスレジスタ間接インデックス付き
      //  (d16,PC)         プログラムカウンタ間接ディスプレースメント付き
      //  (d8,PC,Rn.wl)    プログラムカウンタ間接インデックス付き
      if (asoBasicElement.exlType == ElementType.ETY_PARENTHESIS) {
        //メモリ間接を探す
        //  メモリ間接がないとき
        //    (ベースディスプレースメント,ベースレジスタ,プリインデックス)
        //  メモリ間接があるとき
        //    (アウタディスプレースメント,[ベースディスプレースメント,ベースレジスタ,プリインデックス])
        //    または
        //    (アウタディスプレースメント,[ベースディスプレースメント,ベースレジスタ],ポストインデックス)
        asoMemoryIndirect = false;  //メモリ間接なし
        LinkedList<ExpressionElement> baseList = asoBasicElement.exlParamX.exlToCommaList ();  //全体をベースにする
        LinkedList<ExpressionElement> outerList = null;  //アウタなし
        for (int i = 0, l = baseList.size (); i < l; i++) {
          if (baseList.get (i).exlType == ElementType.ETY_SQUARE_BRACKET) {  // [...]があるとき
            asoMemoryIndirect = true;  //メモリ間接あり
            outerList = baseList;  // [...]の外側をアウタにする
            baseList = outerList.remove (i).exlParamX.exlToCommaList ();  // [...]の内側をベースにする
            break;
          }
        }
        //ベースレジスタを探す
        asoBaseRegisterSuppress = true;
        asoOperandType = ORT_ADDRESS_INDIRECT;
        asoSubscript = 0;  // ZA0
        for (int i = 0, l = baseList.size (); i < l; i++) {
          ExpressionElement e = baseList.get (i);
          ElementType t = e.exlType;
          if (((t == ElementType.ETY_INTEGER_REGISTER ||  //アドレスレジスタ
                t == ElementType.ETY_ZERO_REGISTER) && 8 <= e.exlSubscript) ||  //サプレスされたアドレスレジスタ
              t == ElementType.ETY_PC ||  //プログラムカウンタ
              t == ElementType.ETY_ZERO_PC) {  //サプレスされたプログラムカウンタ
            baseList.remove (i);
            if (t == ElementType.ETY_INTEGER_REGISTER ||  //アドレスレジスタまたは
                t == ElementType.ETY_ZERO_REGISTER) {  //サプレスされたアドレスレジスタ
              asoOperandType = ORT_ADDRESS_INDIRECT;
              asoSubscript = e.exlSubscript - 8;
            } else {  //プログラムカウンタまたはサプレスされたプログラムカウンタ
              asoOperandType = ORT_PROGRAM_INDIRECT;
            }
            asoBaseRegisterSuppress = (t == ElementType.ETY_ZERO_REGISTER ||  //サプレスされたアドレスレジスタまたは
                                       t == ElementType.ETY_ZERO_PC);  //サプレスされたプログラムカウンタ
            break;
          }
        }
        //インデックスを探す
        asoPostindex = false;
        asoIndexSuppress = true;
        asoIndexRegister = 0;
        asoScaleFactorElement = null;
        asoScaleFactorValue = 1;
        asoLog2ScaleFactor = 0;  // ZD0.W*1
        //  プリインデックスを探す
        ExpressionElement indexElement = null;
        for (int i = 0, l = baseList.size (); i < l; i++) {
          ExpressionElement e = baseList.get (i);
          ElementType t = e.exlType;
          if (t == ElementType.ETY_INTEGER_REGISTER ||  // Rn
              (t == ElementType.ETY_SIZE && (e.exlSubscript == 'w' ||
                                             e.exlSubscript == 'l') &&
               e.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) ||  // Rn.WまたはRn.L
              t == ElementType.ETY_SCALE_FACTOR) {  // Rn*SCALEまたはRn.W*SCALEまたはRn.L*SCALE
            indexElement = e;
            baseList.remove (i);
            break;
          }
        }
        //  プリインデックスがなくてメモリ間接があるときポストインデックスを探す
        if (indexElement == null && asoMemoryIndirect) {
          for (int i = 0, l = outerList.size (); i < l; i++) {
            ExpressionElement e = outerList.get (i);
            ElementType t = e.exlType;
            if (t == ElementType.ETY_INTEGER_REGISTER ||  // Rn
                (t == ElementType.ETY_SIZE && (e.exlSubscript == 'w' ||
                                               e.exlSubscript == 'l') &&
                 e.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER) ||  // Rn.WまたはRn.L
                t == ElementType.ETY_SCALE_FACTOR) {  // Rn*SCALEまたはRn.W*SCALEまたはRn.L*SCALE
              asoPostindex = true;
              indexElement = e;
              outerList.remove (i);
              break;
            }
          }
        }
        if (indexElement != null) {  //インデックスが見つかった
          ExpressionElement e = indexElement;
          ElementType t = e.exlType;
          asoIndexSuppress = false;
          asoIndexRegister = (t == ElementType.ETY_INTEGER_REGISTER ? e.exlSubscript :  // Rn
                              t == ElementType.ETY_SIZE ? e.exlParamX.exlSubscript :  // Rn.WまたはRn.L
                              e.exlParamX.exlType == ElementType.ETY_INTEGER_REGISTER ? e.exlParamX.exlSubscript :  // Rn*SCALE
                              e.exlParamX.exlParamX.exlSubscript);  // Rn.W*SCALEまたはRn.L*SCALE
          if (t == ElementType.ETY_SCALE_FACTOR) {  //スケールファクタがある
            asoScaleFactorElement = e.exlParamY;
            asoScaleFactorValue = asoScaleFactorElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
            asoLog2ScaleFactor = (asoScaleFactorValue == 1 ? 0 :
                                  asoScaleFactorValue == 2 ? 1 :
                                  asoScaleFactorValue == 4 ? 2 :
                                  asoScaleFactorValue == 8 ? 3 :
                                  0);  //最終パスは1,2,4,8のいずれでもなければエラーにする
          }
        }
        //ベースディスプレースメントを探す
        asoMinimumBaseDisplacementSize = 0;
        asoMaximumBaseDisplacementSize = 4;
        asoBaseDisplacementElement = null;
        asoBaseDisplacementValue = 0;
        asoBaseDisplacementSize = 0;
        for (int i = 0, l = baseList.size (); i < l; i++) {
          ExpressionElement e = baseList.get (i);
          ElementType t = e.exlType;
          if (e.exlValueType == ElementType.ETY_FLOAT) {  // bd
            asoMinimumBaseDisplacementSize = 0;
            asoMaximumBaseDisplacementSize = 4;
            asoBaseDisplacementElement = e;
            baseList.remove (i);
          } else if (t == ElementType.ETY_SIZE && (e.exlSubscript == 'w' ||
                                                   e.exlSubscript == 'l') &&
                     e.exlParamX.exlType == ElementType.ETY_FLOAT) {  // bd.Wまたはbd.L
            asoBaseDisplacementElement = e.exlParamX;
            if (e.exlSubscript == 'w') {
              asoMinimumBaseDisplacementSize = 2;
              asoMaximumBaseDisplacementSize = 2;
            } else {
              asoMinimumBaseDisplacementSize = 4;
              asoMaximumBaseDisplacementSize = 4;
            }
            baseList.remove (i);
          }
          asoBaseDisplacementValue = asoBaseDisplacementElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
          asoBaseDisplacementSize = (asoBaseDisplacementValue == 0 ? 0 :
                                     !asoMemoryIndirect && (byte) asoBaseDisplacementValue == asoBaseDisplacementValue ? 1 :
                                     (short) asoBaseDisplacementValue == asoBaseDisplacementValue ? 2 :
                                     4);  //最終パスで最大サイズを超えていたらエラー
          break;
        }
        //アウタディスプレースメントを探す
        asoMinimumOuterDisplacementSize = 0;
        asoMaximumOuterDisplacementSize = 4;
        asoOuterDisplacementElement = null;
        asoOuterDisplacementValue = 0;
        asoOuterDisplacementSize = 0;
        for (int i = 0, l = outerList.size (); i < l; i++) {
          ExpressionElement e = outerList.get (i);
          ElementType t = e.exlType;
          if (e.exlValueType == ElementType.ETY_FLOAT) {  // od
            asoMinimumOuterDisplacementSize = 0;
            asoMaximumOuterDisplacementSize = 4;
            asoOuterDisplacementElement = e;
            outerList.remove (i);
          } else if (t == ElementType.ETY_SIZE &&
                     e.exlParamX.exlType == ElementType.ETY_FLOAT) {  // od.Wまたはod.L
            asoOuterDisplacementElement = e.exlParamX;
            if (e.exlSubscript == 'w') {
              asoMinimumOuterDisplacementSize = 2;
              asoMaximumOuterDisplacementSize = 2;
            } else {
              asoMinimumOuterDisplacementSize = 4;
              asoMaximumOuterDisplacementSize = 4;
            }
            outerList.remove (i);
          }
          asoOuterDisplacementValue = asoOuterDisplacementElement.exlEval (EVM_EXPRESSION).exlFloatValue.geti ();
          asoOuterDisplacementSize = (asoOuterDisplacementValue == 0 ? 0 :
                                      (short) asoOuterDisplacementValue == asoOuterDisplacementValue ? 2 :
                                      4);  //最終パスで最大サイズを超えていたらエラー
          break;
        }
        //余分な要素が残っていたらエラー
        if (0 < baseList.size ()) {
          ExpressionElement e = baseList.get (0);
          evxPrintError (Multilingual.mlnJapanese ?
                         "文法エラー" :
                         "syntax error",
                         e.exlSource, e.exlOffset, e.exlLength);
          asoOperandType = ORT_ERROR;
          return;
        }
        if (0 < outerList.size ()) {
          ExpressionElement e = outerList.get (0);
          evxPrintError (Multilingual.mlnJapanese ?
                         "文法エラー" :
                         "syntax error",
                         e.exlSource, e.exlOffset, e.exlLength);
          asoOperandType = ORT_ERROR;
          return;
        }
        return;
      }  //if ETY_PARENTHESIS

      evxPrintError (Multilingual.mlnJapanese ?
                     "文法エラー" :
                     "syntax error",
                     asoBasicElement.exlSource, asoBasicElement.exlOffset, asoBasicElement.exlLength);
      asoOperandType = ORT_ERROR;

    }  //new Operand


    //----------------------------------------------------------------
    //asoUpdate (baseAddress, first, last)
    //  オペランドの中にある式を再計算してオペレーションサイズまたはアドレッシングモードを更新する
    //  初回パスは前回の値を無視して更新フラグを無条件にセットする
    //  最終パスでサイズが最大サイズを超えていたらエラー
    public void asoUpdate (int baseAddress, boolean first, boolean last) {
      asoBaseAddress = baseAddress;
      //!!!
    }


    //----------------------------------------------------------------
    //  文字列化
    public String toString () {
      return this.appendTo (new StringBuilder ()).toString ();
    }
    public StringBuilder appendTo (StringBuilder sb) {
      switch (asoOperandType) {

        //------------------------------------------------
        //  Dr               データレジスタ直接。0..7
      case ORT_DATA_REGISTER:
        return sb.append ('d').append (asoSubscript);

        //------------------------------------------------
        //  Ar               アドレスレジスタ直接。0..7
      case ORT_ADDRESS_REGISTER:
        if (asoSubscript < 7) {
          return sb.append ('a').append (asoSubscript);
        } else {
          return sb.append ("sp");
        }

        //------------------------------------------------
        //  (Ar)+            アドレスレジスタ間接ポストインクリメント付き。0..7
      case ORT_POSTINCREMENT:
        if (asoSubscript < 7) {
          return sb.append ("(a").append (asoSubscript).append (")+");
        } else {
          return sb.append ("(sp)+");
        }

        //------------------------------------------------
        //  -(Ar)            アドレスレジスタ間接プリデクリメント付き。0..7
      case ORT_PREDECREMENT:
        if (asoSubscript < 7) {
          return sb.append ("-(a").append (asoSubscript).append (')');
        } else {
          return sb.append ("-(sp)");
        }

        //------------------------------------------------
        //  #<data>          イミディエイト
      case ORT_IMMEDIATE:
        if (asoOperationSize <= 1) {
          return XEiJ.fmtHex2 (sb.append ('#'), asoImmediateValue);
        } else if (asoOperationSize <= 2) {
          return XEiJ.fmtHex4 (sb.append ('#'), asoImmediateValue);
        } else {
          return XEiJ.fmtHex8 (sb.append ('#'), asoImmediateValue);
        }

        //------------------------------------------------
        //  xxx              絶対アドレス。絶対分岐命令の数値オペランド
        //  (xxx).W          絶対ショート
        //  (xxx).L          絶対ロング
      case ORT_ABSOLUTE_ADDRESS:
        if (asoAbsoluteAddressSize <= 2) {
          return XEiJ.fmtHex4 (sb.append ('$'), asoAbsoluteAddressValue).append (".w");
        } else {
          return XEiJ.fmtHex8 (sb.append ('$'), asoAbsoluteAddressValue).append (".l");
        }

        //------------------------------------------------
        //  xxx              相対アドレス。相対分岐命令の数値オペランド
      case ORT_RELATIVE_ADDRESS:
        return XEiJ.fmtHex8 (sb.append ('$'), asoRelativeAddressValue + asoBaseAddress);

        //------------------------------------------------
        //  CCR              コンディションコードレジスタ
      case ORT_CONDITION_CODE:
        return sb.append ("ccr");

        //------------------------------------------------
        //  SR               ステータスレジスタ
      case ORT_STATUS_REGISTER:
        return sb.append ("sr");

        //------------------------------------------------
        //  D0-D7/A0-A7      レジスタリスト。0x0000..0xffff
      case ORT_REGISTER_LIST:
        {
          int m = asoSubscript;
          m = (m & 0x8000) << 2 | (m & 0x7f00) << 1 | (m & 0x00ff);  //D7/A0とA6/SPを不連続にする
          boolean s = false;
          while (m != 0) {
            int i = Integer.numberOfTrailingZeros (m);
            m += 1 << i;
            int j = Integer.numberOfTrailingZeros (m);
            m -= 1 << j;
            j--;
            if (s) {
              sb.append ('/');
            }
            if (i <= 7) {
              sb.append ('d').append (i);
            } else if (i <= 16) {
              sb.append ('a').append (i - 9);
            } else {
              sb.append ("sp");
            }
            if (i < j) {
              sb.append ('-');
              if (j <= 7) {
                sb.append ('d').append (j);
              } else if (j <= 16) {
                sb.append ('a').append (j - 9);
              } else {
                sb.append ("sp");
              }
            }
            s = true;
          }  //while m!=0
          return sb;
        }

        //------------------------------------------------
        //  Dh:Dl            データレジスタペア。0x00..0x3f
      case ORT_REGISTER_PAIR:
        {
          int h = asoSubscript >> 3;
          int l = asoSubscript & 7;
          return sb.append ('d').append (h).append (":d").append (l);
        }

        //------------------------------------------------
        //  (Rr):(Rs)        レジスタ間接ペア。0x00..0xff
      case ORT_INDIRECT_PAIR:
        {
          int r = asoSubscript >> 4;
          int s = asoSubscript & 15;
          sb.append ('(');
          if (r <= 7) {
            sb.append ('d').append (r);
          } else if (r <= 14) {
            sb.append ('a').append (r - 8);
          } else {
            sb.append ("sp");
          }
          sb.append ("):(");
          if (s <= 7) {
            sb.append ('d').append (s);
          } else if (s <= 14) {
            sb.append ('a').append (s - 8);
          } else {
            sb.append ("sp");
          }
          return sb.append (')');
        }

        //------------------------------------------------
        //  FPn              浮動小数点レジスタ。0..7
      case ORT_FLOAT_REGISTER:
        return sb.append ("fp").append (asoSubscript);

        //------------------------------------------------
        //  FP0-FP7          浮動小数点レジスタリスト。0x00..0xff
      case ORT_FLOAT_LIST:
        {
          int m = asoSubscript;
          boolean s = false;
          while (m != 0) {
            int i = Integer.numberOfTrailingZeros (m);
            m += 1 << i;
            int j = Integer.numberOfTrailingZeros (m);
            m -= 1 << j;
            j--;
            if (s) {
              sb.append ('/');
            }
            sb.append ("fp").append (i);
            if (i < j) {
              sb.append ("-fp").append (j);
            }
            s = true;
          }  //while m!=0
          return sb;
        }

        //------------------------------------------------
        //  FPIAR/FPSR/FPCR  浮動小数点制御レジスタ(リスト)。0..7
      case ORT_FLOAT_CONTROL:
        {
          int m = asoSubscript;
          boolean s = false;
          while (m != 0) {
            int i = Integer.numberOfTrailingZeros (m);
            m -= 1 << i;
            if (s) {
              sb.append ('/');
            }
            sb.append (EVX_FLOAT_CONTROL_NAME_ARRAY[i]);
            s = true;
          }  //while m!=0
          return sb;
        }

        //------------------------------------------------
        //  NC DC IC BC      キャッシュ選択。0..3
      case ORT_CACHE_SELECTION:
        return sb.append (EVX_CACHE_NAME_ARRAY[asoSubscript]);

        //------------------------------------------------
        //  SFC ...          制御レジスタ(MOVEC)。0x0000..0xffff
      case ORT_CONTROL_REGISTER:
        return sb.append (EVX_CONTROL_MPU_CODE_TO_NAME.get (asoSubscript));

        //------------------------------------------------
        //  TT0 ...          メモリ管理レジスタ(PMOVE)。0x0000..0xffff
        //case ORT_MMU_REGISTER:
        //  return sb.append (EVX_MMU_CODE_TO_NAME.get (asoSubscript));

        //------------------------------------------------
        //  (Ar)             アドレスレジスタ間接。0..7
        //  (d16,Ar)         アドレスレジスタ間接ディスプレースメント付き。0..7
        //  (d8,Ar,Rn.wl)    アドレスレジスタ間接インデックス付き。0..7
        //                   プログラムカウンタ間接
        //  (d16,PC)         プログラムカウンタ間接ディスプレースメント付き
        //  (d8,PC,Rn.wl)    プログラムカウンタ間接インデックス付き
      case ORT_ADDRESS_INDIRECT:
      case ORT_PROGRAM_INDIRECT:
        if (!asoBaseRegisterSuppress &&
            asoBaseDisplacementSize <= 1 &&
            !asoMemoryIndirect &&
            !asoIndexSuppress) {  //ブリーフフォーマット
          sb.append ('(');
          //  ベースディスプレースメント
          if (asoOperandType == ORT_PROGRAM_INDIRECT) {
            XEiJ.fmtHex8 (sb.append ('$'), asoBaseDisplacementValue + asoBaseAddress);
          } else {
            XEiJ.fmtHex2 (sb.append ('$'), asoBaseDisplacementValue);
          }
          sb.append (',');
          //  ベースレジスタ
          if (asoOperandType == ORT_PROGRAM_INDIRECT) {
            sb.append ("pc");
          } else if (asoSubscript < 7) {
            sb.append ('a').append (asoSubscript);
          } else {
            sb.append ("sp");
          }
          sb.append (',');
          //  インデックス
          if (asoIndexRegister < 8) {
            sb.append ('d').append (asoIndexRegister);
          } else if (asoIndexRegister < 15) {
            sb.append ('a').append (asoIndexRegister - 8);
          } else {
            sb.append ("sp");
          }
          if (0 < asoLog2ScaleFactor) {
            sb.append ('*').append (1 << asoLog2ScaleFactor);
          }
          sb.append (')');
        } else {  //フルフォーマット
          sb.append ('(');
          //  メモリ間接
          if (asoMemoryIndirect) {
            sb.append ('[');
          }
          //  ベースディスプレースメント
          if (0 < asoBaseDisplacementSize) {
            if (asoOperandType == ORT_PROGRAM_INDIRECT) {
              XEiJ.fmtHex8 (sb.append ('$'), asoBaseDisplacementValue + asoBaseAddress);
            } else if (asoBaseDisplacementSize <= 2) {
              XEiJ.fmtHex4 (sb.append ('$'), asoBaseDisplacementValue);
            } else {
              XEiJ.fmtHex8 (sb.append ('$'), asoBaseDisplacementValue);
            }
            sb.append (',');
          }
          //  ベースレジスタ
          if (asoBaseRegisterSuppress) {
            sb.append ('z');
          }
          if (asoOperandType == ORT_PROGRAM_INDIRECT) {
            sb.append ("pc");
          } else if (asoSubscript < 7) {
            sb.append ('a').append (asoSubscript);
          } else {
            sb.append ("sp");
          }
          //  プリインデックス
          if (!asoIndexSuppress && !asoPostindex) {
            sb.append (',');
            if (asoIndexRegister < 8) {
              sb.append ('d').append (asoIndexRegister);
            } else if (asoIndexRegister < 15) {
              sb.append ('a').append (asoIndexRegister - 8);
            } else {
              sb.append ("sp");
            }
            if (0 < asoLog2ScaleFactor) {
              sb.append ('*').append (1 << asoLog2ScaleFactor);
            }
          }
          //  メモリ間接
          if (asoMemoryIndirect) {
            sb.append (']');
            //  アウタディスプレースメント
            if (0 < asoOuterDisplacementSize) {
              if (asoBaseDisplacementSize <= 2) {
                XEiJ.fmtHex4 (sb.append ('$'), asoOuterDisplacementValue);
              } else {
                XEiJ.fmtHex8 (sb.append ('$'), asoOuterDisplacementValue);
              }
              sb.append (',');
            }
            //  ポストインデックス
            if (!asoIndexSuppress && asoPostindex) {
              sb.append (',');
              if (asoIndexRegister < 8) {
                sb.append ('d').append (asoIndexRegister);
              } else if (asoIndexRegister < 15) {
                sb.append ('a').append (asoIndexRegister - 8);
              } else {
                sb.append ("sp");
              }
              if (0 < asoLog2ScaleFactor) {
                sb.append ('*').append (1 << asoLog2ScaleFactor);
              }
            }
          }
          sb.append (')');
        }  //if ブリーフフォーマット/フルフォーマット
        return sb;

      }  //switch asoOperandType

      return sb.append ("???");

    }  //appendTo


  }  //class Operand



}  //class ExpressionEvaluator



