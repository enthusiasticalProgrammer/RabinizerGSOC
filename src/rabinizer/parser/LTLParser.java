/* LTLParser.java */
/* Generated By:JavaCC: Do not edit this line. LTLParser.java */
package rabinizer.parser;

import rabinizer.formulas.*;

import java.util.ArrayList;
import java.util.Arrays;

import rabinizer.bdd.*;

public class LTLParser implements LTLParserConstants {
  /**
   * Parse with new bijection between identifiers and atoms.
   */
  public Formula parse() throws ParseException
  {
    BDDForVariables.bijectionIdAtom = new BijectionIdAtom();
    return formula();
  }

 /**
  * Parse using the previous bijection between identifiers and atoms.
  * If there is no previous bijection, create a new bijection. 
  */
  public Formula parsePreviousAtoms() throws ParseException
  {
        if (BDDForVariables.bijectionIdAtom == null) BDDForVariables.bijectionIdAtom = new BijectionIdAtom();
        return formula();
  }

  @SuppressWarnings("unused")
final public Formula formula() throws ParseException {Formula f;
    f = disjunction();
    jj_consume_token(0);
{if ("" != null) return f;}
    throw new Error("Missing return statement in function");
  }

  @SuppressWarnings("unused")
final public Formula disjunction() throws ParseException {Formula r = null;
  Formula result;
    result = conjunction();
    label_1:
    while (true) {
      if (jj_2_1(2)) {
        ;
      } else {
        break label_1;
      }
      jj_consume_token(OR);
      r = conjunction();
result = FormulaFactory.mkOr(result, r);
    }
{if ("" != null) return result;}
    throw new Error("Missing return statement in function");
  }

  @SuppressWarnings("unused")
final public Formula conjunction() throws ParseException {Formula result;
  Formula r = null;
    result = until();
    label_2:
    while (true) {
      if (jj_2_2(2)) {
        ;
      } else {
        break label_2;
      }
      jj_consume_token(AND);
      r = until();
result = FormulaFactory.mkAnd(result, r);
    }
{if ("" != null) return result;}
    throw new Error("Missing return statement in function");
  }

  @SuppressWarnings("unused")
final public Formula until() throws ParseException {Formula result;
  Formula r = null;
    result = unaryOp();
    label_3:
    while (true) {
      if (jj_2_3(2)) {
        ;
      } else {
        break label_3;
      }
      jj_consume_token(UOP);
      r = unaryOp();
result = FormulaFactory.mkU(result, r);
    }
{if ("" != null) return result;}
    throw new Error("Missing return statement in function");
  }

//Formula negation() :
//{
//  Formula f;
//  boolean neg = false;
//}
//{
///*  (< NEG >
//  {
//    neg = true;
//  }
//  )?
//*/
//  f = tempOp()
//  {
//    if (neg) return f.negated();
//    else return f;
//  }
//}
  @SuppressWarnings("unused")
final public 
Formula unaryOp() throws ParseException {Formula f;
    if (jj_2_4(2)) {
      jj_consume_token(FOP);
      f = unaryOp();
{if ("" != null) return FormulaFactory.mkF(f);}
    } else if (jj_2_5(2)) {
      jj_consume_token(GOP);
      f = unaryOp();
{if ("" != null) return FormulaFactory.mkG(f);}
    } else if (jj_2_6(2)) {
      jj_consume_token(XOP);
      f = unaryOp();
{if ("" != null) return FormulaFactory.mkX(f);}
    } else if (jj_2_7(2)) {
      jj_consume_token(NEG);
      f = unaryOp();
{if ("" != null) return FormulaFactory.mkNot(f);}
    } else if (jj_2_8(2)) {
      f = atom();
{if ("" != null) return f;}
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  @SuppressWarnings("unused")
final public Formula atom() throws ParseException {String atomString;
  int id;
  Formula f;
    if (jj_2_9(2)) {
      jj_consume_token(TRUE);
{if ("" != null) return FormulaFactory.mkConst(true);}
    } else if (jj_2_10(2)) {
      jj_consume_token(FALSE);
{if ("" != null) return FormulaFactory.mkConst(false);}
    } else if (jj_2_11(2)) {
      atomString = jj_consume_token(ID).image;
id = BDDForVariables.bijectionIdAtom.id(atomString);
        {if ("" != null) return FormulaFactory.mkLit(atomString, id, false);}
    } else if (jj_2_12(2)) {
      jj_consume_token(LPAR);
      f = disjunction();
      jj_consume_token(RPAR);
{if ("" != null) return f;}
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  private boolean jj_2_1(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  private boolean jj_2_2(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_2(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(1, xla); }
  }

  private boolean jj_2_3(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_3(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(2, xla); }
  }

  private boolean jj_2_4(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_4(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(3, xla); }
  }

  private boolean jj_2_5(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_5(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(4, xla); }
  }

  private boolean jj_2_6(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_6(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(5, xla); }
  }

  private boolean jj_2_7(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_7(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(6, xla); }
  }

  private boolean jj_2_8(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_8(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(7, xla); }
  }

  private boolean jj_2_9(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_9(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(8, xla); }
  }

  private boolean jj_2_10(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_10(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(9, xla); }
  }

  private boolean jj_2_11(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_11(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(10, xla); }
  }

  private boolean jj_2_12(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_12(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(11, xla); }
  }

  private boolean jj_3_5()
 {
    if (jj_scan_token(GOP)) return true;
    if (jj_3R_6()) return true;
    return false;
  }

  private boolean jj_3_1()
 {
    if (jj_scan_token(OR)) return true;
    if (jj_3R_4()) return true;
    return false;
  }

  private boolean jj_3R_6()
 {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_4()) {
    jj_scanpos = xsp;
    if (jj_3_5()) {
    jj_scanpos = xsp;
    if (jj_3_6()) {
    jj_scanpos = xsp;
    if (jj_3_7()) {
    jj_scanpos = xsp;
    if (jj_3_8()) return true;
    }
    }
    }
    }
    return false;
  }

  private boolean jj_3_4()
 {
    if (jj_scan_token(FOP)) return true;
    if (jj_3R_6()) return true;
    return false;
  }

  private boolean jj_3R_8()
 {
    if (jj_3R_4()) return true;
    return false;
  }

  private boolean jj_3_12()
 {
    if (jj_scan_token(LPAR)) return true;
    if (jj_3R_8()) return true;
    return false;
  }

  private boolean jj_3_2()
 {
    if (jj_scan_token(AND)) return true;
    if (jj_3R_5()) return true;
    return false;
  }

  private boolean jj_3_8()
 {
    if (jj_3R_7()) return true;
    return false;
  }

  private boolean jj_3R_4()
 {
    if (jj_3R_5()) return true;
    return false;
  }

  private boolean jj_3_11()
 {
    if (jj_scan_token(ID)) return true;
    return false;
  }

  private boolean jj_3_3()
 {
    if (jj_scan_token(UOP)) return true;
    if (jj_3R_6()) return true;
    return false;
  }

  private boolean jj_3_7()
 {
    if (jj_scan_token(NEG)) return true;
    if (jj_3R_6()) return true;
    return false;
  }

  private boolean jj_3R_5()
 {
    if (jj_3R_6()) return true;
    return false;
  }

  private boolean jj_3_10()
 {
    if (jj_scan_token(FALSE)) return true;
    return false;
  }

  private boolean jj_3_6()
 {
    if (jj_scan_token(XOP)) return true;
    if (jj_3R_6()) return true;
    return false;
  }

  private boolean jj_3R_7()
 {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_9()) {
    jj_scanpos = xsp;
    if (jj_3_10()) {
    jj_scanpos = xsp;
    if (jj_3_11()) {
    jj_scanpos = xsp;
    if (jj_3_12()) return true;
    }
    }
    }
    return false;
  }

  private boolean jj_3_9()
 {
    if (jj_scan_token(TRUE)) return true;
    return false;
  }

  /** Generated Token Manager. */
  public LTLParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  private int jj_gen;
  final private int[] jj_la1 = new int[0];
  static private int[] jj_la1_0;
  static {
      jj_la1_init_0();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[12];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with InputStream. */
  public LTLParser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public LTLParser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new LTLParserTokenManager(jj_input_stream);
    token = new Token();
    jj_gen = 0;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public LTLParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new LTLParserTokenManager(jj_input_stream);
    token = new Token();
    jj_gen = 0;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_gen = 0;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public LTLParser(LTLParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_gen = 0;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(LTLParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_gen = 0;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  @SuppressWarnings("serial")
  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
        int[] oldentry = (int[])(it.next());
        if (oldentry.length == jj_expentry.length) {
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              continue jj_entries_loop;
            }
          }
          jj_expentries.add(jj_expentry);
          break jj_entries_loop;
        }
      }
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[17];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 0; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 17; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 12; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
            case 1: jj_3_2(); break;
            case 2: jj_3_3(); break;
            case 3: jj_3_4(); break;
            case 4: jj_3_5(); break;
            case 5: jj_3_6(); break;
            case 6: jj_3_7(); break;
            case 7: jj_3_8(); break;
            case 8: jj_3_9(); break;
            case 9: jj_3_10(); break;
            case 10: jj_3_11(); break;
            case 11: jj_3_12(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}
