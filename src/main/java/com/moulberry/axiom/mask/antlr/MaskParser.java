package com.moulberry.axiom.mask.antlr;

import java.util.List;
import org.antlr.axiom.v4.runtime.FailedPredicateException;
import org.antlr.axiom.v4.runtime.NoViableAltException;
import org.antlr.axiom.v4.runtime.Parser;
import org.antlr.axiom.v4.runtime.ParserRuleContext;
import org.antlr.axiom.v4.runtime.RecognitionException;
import org.antlr.axiom.v4.runtime.RuleContext;
import org.antlr.axiom.v4.runtime.RuntimeMetaData;
import org.antlr.axiom.v4.runtime.Token;
import org.antlr.axiom.v4.runtime.TokenStream;
import org.antlr.axiom.v4.runtime.Vocabulary;
import org.antlr.axiom.v4.runtime.VocabularyImpl;
import org.antlr.axiom.v4.runtime.atn.ATN;
import org.antlr.axiom.v4.runtime.atn.ATNDeserializer;
import org.antlr.axiom.v4.runtime.atn.ParserATNSimulator;
import org.antlr.axiom.v4.runtime.atn.PredictionContextCache;
import org.antlr.axiom.v4.runtime.dfa.DFA;
import org.antlr.axiom.v4.runtime.tree.ParseTreeListener;
import org.antlr.axiom.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.axiom.v4.runtime.tree.TerminalNode;

public class MaskParser extends Parser {
   protected static final DFA[] _decisionToDFA;
   protected static final PredictionContextCache _sharedContextCache = new PredictionContextCache();
   public static final int T__0 = 1;
   public static final int T__1 = 2;
   public static final int T__2 = 3;
   public static final int T__3 = 4;
   public static final int T__4 = 5;
   public static final int T__5 = 6;
   public static final int T__6 = 7;
   public static final int T__7 = 8;
   public static final int T__8 = 9;
   public static final int T__9 = 10;
   public static final int T__10 = 11;
   public static final int T__11 = 12;
   public static final int T__12 = 13;
   public static final int T__13 = 14;
   public static final int T__14 = 15;
   public static final int T__15 = 16;
   public static final int T__16 = 17;
   public static final int T__17 = 18;
   public static final int T__18 = 19;
   public static final int T__19 = 20;
   public static final int T__20 = 21;
   public static final int T__21 = 22;
   public static final int T__22 = 23;
   public static final int T__23 = 24;
   public static final int T__24 = 25;
   public static final int T__25 = 26;
   public static final int T__26 = 27;
   public static final int T__27 = 28;
   public static final int UNSIGNED_INTEGER = 29;
   public static final int BLOCK = 30;
   public static final int BIOME = 31;
   public static final int IDENTIFIER_FRAGMENT = 32;
   public static final int POW = 33;
   public static final int PLUS = 34;
   public static final int MINUS = 35;
   public static final int MULTIPLY = 36;
   public static final int DIVIDE = 37;
   public static final int EQUALS = 38;
   public static final int LT = 39;
   public static final int LT_EQ = 40;
   public static final int GT = 41;
   public static final int GT_EQ = 42;
   public static final int NOT_EQUALS = 43;
   public static final int WS = 44;
   public static final int RULE_mask = 0;
   public static final int RULE_maskElement = 1;
   public static final int RULE_multiBiomeMatch = 2;
   public static final int RULE_multiBlockMatch = 3;
   public static final int RULE_blockMatch = 4;
   public static final int RULE_property = 5;
   public static final int RULE_near = 6;
   public static final int RULE_single = 7;
   public static final int RULE_cmpBlock = 8;
   public static final int RULE_cmpBiome = 9;
   public static final int RULE_cmpNumeric = 10;
   public static final int RULE_numeric = 11;
   public static final int RULE_identifier = 12;
   public static final String[] ruleNames = new String[]{
      "mask",
      "maskElement",
      "multiBiomeMatch",
      "multiBlockMatch",
      "blockMatch",
      "property",
      "near",
      "single",
      "cmpBlock",
      "cmpBiome",
      "cmpNumeric",
      "numeric",
      "identifier"
   };
   private static final String[] _LITERAL_NAMES = new String[]{
      null,
      "'offset'",
      "'('",
      "','",
      "')'",
      "'{'",
      "'}'",
      "'!'",
      "'&'",
      "'&&'",
      "'and'",
      "'|'",
      "'||'",
      "'or'",
      "'['",
      "']'",
      "'#'",
      "'near'",
      "'sky'",
      "'selected'",
      "'surface'",
      "'above'",
      "'below'",
      "'neighbor'",
      "'adjacent'",
      "'x'",
      "'y'",
      "'z'",
      "'angle'",
      null,
      "'block'",
      "'biome'",
      null,
      null,
      "'+'",
      "'-'",
      "'*'",
      "'/'",
      null,
      "'<'",
      "'<='",
      "'>'",
      "'>='",
      "'!='"
   };
   private static final String[] _SYMBOLIC_NAMES = new String[]{
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "UNSIGNED_INTEGER",
      "BLOCK",
      "BIOME",
      "IDENTIFIER_FRAGMENT",
      "POW",
      "PLUS",
      "MINUS",
      "MULTIPLY",
      "DIVIDE",
      "EQUALS",
      "LT",
      "LT_EQ",
      "GT",
      "GT_EQ",
      "NOT_EQUALS",
      "WS"
   };
   public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);
   @Deprecated
   public static final String[] tokenNames = new String[_SYMBOLIC_NAMES.length];
   public static final String _serializedATN = "\u0003悋Ꜫ脳맭䅼㯧瞆奤\u0003.À\u0004\u0002\t\u0002\u0004\u0003\t\u0003\u0004\u0004\t\u0004\u0004\u0005\t\u0005\u0004\u0006\t\u0006\u0004\u0007\t\u0007\u0004\b\t\b\u0004\t\t\t\u0004\n\t\n\u0004\u000b\t\u000b\u0004\f\t\f\u0004\r\t\r\u0004\u000e\t\u000e\u0003\u0002\u0003\u0002\u0003\u0002\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0005\u0003@\n\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0007\u0003H\n\u0003\f\u0003\u000e\u0003K\u000b\u0003\u0003\u0004\u0003\u0004\u0003\u0004\u0003\u0004\u0003\u0004\u0007\u0004R\n\u0004\f\u0004\u000e\u0004U\u000b\u0004\u0003\u0004\u0005\u0004X\n\u0004\u0003\u0004\u0003\u0004\u0005\u0004\\\n\u0004\u0003\u0005\u0003\u0005\u0003\u0005\u0003\u0005\u0003\u0005\u0007\u0005c\n\u0005\f\u0005\u000e\u0005f\u000b\u0005\u0003\u0005\u0005\u0005i\n\u0005\u0003\u0005\u0003\u0005\u0005\u0005m\n\u0005\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0007\u0006v\n\u0006\f\u0006\u000e\u0006y\u000b\u0006\u0003\u0006\u0003\u0006\u0005\u0006}\n\u0006\u0005\u0006\u007f\n\u0006\u0003\u0007\u0003\u0007\u0003\u0007\u0003\u0007\u0007\u0007\u0085\n\u0007\f\u0007\u000e\u0007\u0088\u000b\u0007\u0003\u0007\u0005\u0007\u008b\n\u0007\u0003\b\u0003\b\u0003\b\u0003\b\u0003\b\u0005\b\u0092\n\b\u0003\t\u0003\t\u0003\n\u0003\n\u0003\n\u0003\n\u0003\n\u0003\n\u0005\n\u009c\n\n\u0003\u000b\u0003\u000b\u0003\f\u0003\f\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0007\r¨\n\r\f\r\u000e\r«\u000b\r\u0003\r\u0005\r®\n\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0007\r¹\n\r\f\r\u000e\r¼\u000b\r\u0003\u000e\u0003\u000e\u0003\u000e\u0002\u0004\u0004\u0018\u000f\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u0002\n\u0004\u0002((--\u0003\u0002(-\u0003\u0002\n\f\u0003\u0002\r\u000f\u0003\u0002$%\u0003\u0002\u0014\u0016\u0003\u0002\u001b\u001e\u0003\u0002&'\u0002Ð\u0002\u001c\u0003\u0002\u0002\u0002\u0004?\u0003\u0002\u0002\u0002\u0006[\u0003\u0002\u0002\u0002\bl\u0003\u0002\u0002\u0002\n~\u0003\u0002\u0002\u0002\f\u0080\u0003\u0002\u0002\u0002\u000e\u008c\u0003\u0002\u0002\u0002\u0010\u0093\u0003\u0002\u0002\u0002\u0012\u009b\u0003\u0002\u0002\u0002\u0014\u009d\u0003\u0002\u0002\u0002\u0016\u009f\u0003\u0002\u0002\u0002\u0018\u00ad\u0003\u0002\u0002\u0002\u001a½\u0003\u0002\u0002\u0002\u001c\u001d\u0005\u0004\u0003\u0002\u001d\u001e\u0007\u0002\u0002\u0003\u001e\u0003\u0003\u0002\u0002\u0002\u001f \b\u0003\u0001\u0002 !\u0007\u0003\u0002\u0002!\"\u0007\u0004\u0002\u0002\"#\u0005\u0018\r\u0002#$\u0007\u0005\u0002\u0002$%\u0005\u0018\r\u0002%&\u0007\u0005\u0002\u0002&'\u0005\u0018\r\u0002'(\u0007\u0006\u0002\u0002()\u0007\u0007\u0002\u0002)*\u0005\u0004\u0003\u0002*+\u0007\b\u0002\u0002+@\u0003\u0002\u0002\u0002,@\u0005\u0010\t\u0002-.\u0005\u0012\n\u0002./\t\u0002\u0002\u0002/0\u0005\b\u0005\u00020@\u0003\u0002\u0002\u000212\u0005\u0014\u000b\u000223\t\u0002\u0002\u000234\u0005\u0006\u0004\u00024@\u0003\u0002\u0002\u000256\u0005\u0016\f\u000267\t\u0003\u0002\u000278\u0005\u0018\r\u00028@\u0003\u0002\u0002\u00029:\u0007\t\u0002\u0002:@\u0005\u0004\u0003\u0006;<\u0007\u0004\u0002\u0002<=\u0005\u0004\u0003\u0002=>\u0007\u0006\u0002\u0002>@\u0003\u0002\u0002\u0002?\u001f\u0003\u0002\u0002\u0002?,\u0003\u0002\u0002\u0002?-\u0003\u0002\u0002\u0002?1\u0003\u0002\u0002\u0002?5\u0003\u0002\u0002\u0002?9\u0003\u0002\u0002\u0002?;\u0003\u0002\u0002\u0002@I\u0003\u0002\u0002\u0002AB\f\u0005\u0002\u0002BC\t\u0004\u0002\u0002CH\u0005\u0004\u0003\u0006DE\f\u0004\u0002\u0002EF\t\u0005\u0002\u0002FH\u0005\u0004\u0003\u0005GA\u0003\u0002\u0002\u0002GD\u0003\u0002\u0002\u0002HK\u0003\u0002\u0002\u0002IG\u0003\u0002\u0002\u0002IJ\u0003\u0002\u0002\u0002J\u0005\u0003\u0002\u0002\u0002KI\u0003\u0002\u0002\u0002L\\\u0005\u001a\u000e\u0002MN\u0007\u0010\u0002\u0002NS\u0005\u001a\u000e\u0002OP\u0007\u0005\u0002\u0002PR\u0005\u001a\u000e\u0002QO\u0003\u0002\u0002\u0002RU\u0003\u0002\u0002\u0002SQ\u0003\u0002\u0002\u0002ST\u0003\u0002\u0002\u0002TW\u0003\u0002\u0002\u0002US\u0003\u0002\u0002\u0002VX\u0007\u0005\u0002\u0002WV\u0003\u0002\u0002\u0002WX\u0003\u0002\u0002\u0002XY\u0003\u0002\u0002\u0002YZ\u0007\u0011\u0002\u0002Z\\\u0003\u0002\u0002\u0002[L\u0003\u0002\u0002\u0002[M\u0003\u0002\u0002\u0002\\\u0007\u0003\u0002\u0002\u0002]m\u0005\n\u0006\u0002^_\u0007\u0010\u0002\u0002_d\u0005\n\u0006\u0002`a\u0007\u0005\u0002\u0002ac\u0005\n\u0006\u0002b`\u0003\u0002\u0002\u0002cf\u0003\u0002\u0002\u0002db\u0003\u0002\u0002\u0002de\u0003\u0002\u0002\u0002eh\u0003\u0002\u0002\u0002fd\u0003\u0002\u0002\u0002gi\u0007\u0005\u0002\u0002hg\u0003\u0002\u0002\u0002hi\u0003\u0002\u0002\u0002ij\u0003\u0002\u0002\u0002jk\u0007\u0011\u0002\u0002km\u0003\u0002\u0002\u0002l]\u0003\u0002\u0002\u0002l^\u0003\u0002\u0002\u0002m\t\u0003\u0002\u0002\u0002no\u0007\u0012\u0002\u0002o\u007f\u0005\u001a\u000e\u0002p|\u0005\u001a\u000e\u0002qr\u0007\u0010\u0002\u0002rw\u0005\f\u0007\u0002st\u0007\u0005\u0002\u0002tv\u0005\f\u0007\u0002us\u0003\u0002\u0002\u0002vy\u0003\u0002\u0002\u0002wu\u0003\u0002\u0002\u0002wx\u0003\u0002\u0002\u0002xz\u0003\u0002\u0002\u0002yw\u0003\u0002\u0002\u0002z{\u0007\u0011\u0002\u0002{}\u0003\u0002\u0002\u0002|q\u0003\u0002\u0002\u0002|}\u0003\u0002\u0002\u0002}\u007f\u0003\u0002\u0002\u0002~n\u0003\u0002\u0002\u0002~p\u0003\u0002\u0002\u0002\u007f\u000b\u0003\u0002\u0002\u0002\u0080\u0081\u0005\u001a\u000e\u0002\u0081\u008a\u0007(\u0002\u0002\u0082\u008b\u0005\u001a\u000e\u0002\u0083\u0085\t\u0006\u0002\u0002\u0084\u0083\u0003\u0002\u0002\u0002\u0085\u0088\u0003\u0002\u0002\u0002\u0086\u0084\u0003\u0002\u0002\u0002\u0086\u0087\u0003\u0002\u0002\u0002\u0087\u0089\u0003\u0002\u0002\u0002\u0088\u0086\u0003\u0002\u0002\u0002\u0089\u008b\u0007\u001f\u0002\u0002\u008a\u0082\u0003\u0002\u0002\u0002\u008a\u0086\u0003\u0002\u0002\u0002\u008b\r\u0003\u0002\u0002\u0002\u008c\u0091\u0007\u0013\u0002\u0002\u008d\u008e\u0007\u0004\u0002\u0002\u008e\u008f\u0005\u0018\r\u0002\u008f\u0090\u0007\u0006\u0002\u0002\u0090\u0092\u0003\u0002\u0002\u0002\u0091\u008d\u0003\u0002\u0002\u0002\u0091\u0092\u0003\u0002\u0002\u0002\u0092\u000f\u0003\u0002\u0002\u0002\u0093\u0094\t\u0007\u0002\u0002\u0094\u0011\u0003\u0002\u0002\u0002\u0095\u009c\u0007 \u0002\u0002\u0096\u009c\u0007\u0017\u0002\u0002\u0097\u009c\u0007\u0018\u0002\u0002\u0098\u009c\u0005\u000e\b\u0002\u0099\u009c\u0007\u0019\u0002\u0002\u009a\u009c\u0007\u001a\u0002\u0002\u009b\u0095\u0003\u0002\u0002\u0002\u009b\u0096\u0003\u0002\u0002\u0002\u009b\u0097\u0003\u0002\u0002\u0002\u009b\u0098\u0003\u0002\u0002\u0002\u009b\u0099\u0003\u0002\u0002\u0002\u009b\u009a\u0003\u0002\u0002\u0002\u009c\u0013\u0003\u0002\u0002\u0002\u009d\u009e\u0007!\u0002\u0002\u009e\u0015\u0003\u0002\u0002\u0002\u009f \t\b\u0002\u0002 \u0017\u0003\u0002\u0002\u0002¡¢\b\r\u0001\u0002¢£\u0007\u0004\u0002\u0002£¤\u0005\u0018\r\u0002¤¥\u0007\u0006\u0002\u0002¥®\u0003\u0002\u0002\u0002¦¨\t\u0006\u0002\u0002§¦\u0003\u0002\u0002\u0002¨«\u0003\u0002\u0002\u0002©§\u0003\u0002\u0002\u0002©ª\u0003\u0002\u0002\u0002ª¬\u0003\u0002\u0002\u0002«©\u0003\u0002\u0002\u0002¬®\u0007\u001f\u0002\u0002\u00ad¡\u0003\u0002\u0002\u0002\u00ad©\u0003\u0002\u0002\u0002®º\u0003\u0002\u0002\u0002¯°\f\u0007\u0002\u0002°±\u0007#\u0002\u0002±¹\u0005\u0018\r\b²³\f\u0006\u0002\u0002³´\t\t\u0002\u0002´¹\u0005\u0018\r\u0007µ¶\f\u0005\u0002\u0002¶·\t\u0006\u0002\u0002·¹\u0005\u0018\r\u0006¸¯\u0003\u0002\u0002\u0002¸²\u0003\u0002\u0002\u0002¸µ\u0003\u0002\u0002\u0002¹¼\u0003\u0002\u0002\u0002º¸\u0003\u0002\u0002\u0002º»\u0003\u0002\u0002\u0002»\u0019\u0003\u0002\u0002\u0002¼º\u0003\u0002\u0002\u0002½¾\u0007\"\u0002\u0002¾\u001b\u0003\u0002\u0002\u0002\u0016?GISW[dhlw|~\u0086\u008a\u0091\u009b©\u00ad¸º";
   public static final ATN _ATN;

   @Deprecated
   public String[] getTokenNames() {
      return tokenNames;
   }

   public Vocabulary getVocabulary() {
      return VOCABULARY;
   }

   public String getGrammarFileName() {
      return "Mask.g4";
   }

   public String[] getRuleNames() {
      return ruleNames;
   }

   public String getSerializedATN() {
      return "\u0003悋Ꜫ脳맭䅼㯧瞆奤\u0003.À\u0004\u0002\t\u0002\u0004\u0003\t\u0003\u0004\u0004\t\u0004\u0004\u0005\t\u0005\u0004\u0006\t\u0006\u0004\u0007\t\u0007\u0004\b\t\b\u0004\t\t\t\u0004\n\t\n\u0004\u000b\t\u000b\u0004\f\t\f\u0004\r\t\r\u0004\u000e\t\u000e\u0003\u0002\u0003\u0002\u0003\u0002\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0005\u0003@\n\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0007\u0003H\n\u0003\f\u0003\u000e\u0003K\u000b\u0003\u0003\u0004\u0003\u0004\u0003\u0004\u0003\u0004\u0003\u0004\u0007\u0004R\n\u0004\f\u0004\u000e\u0004U\u000b\u0004\u0003\u0004\u0005\u0004X\n\u0004\u0003\u0004\u0003\u0004\u0005\u0004\\\n\u0004\u0003\u0005\u0003\u0005\u0003\u0005\u0003\u0005\u0003\u0005\u0007\u0005c\n\u0005\f\u0005\u000e\u0005f\u000b\u0005\u0003\u0005\u0005\u0005i\n\u0005\u0003\u0005\u0003\u0005\u0005\u0005m\n\u0005\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0007\u0006v\n\u0006\f\u0006\u000e\u0006y\u000b\u0006\u0003\u0006\u0003\u0006\u0005\u0006}\n\u0006\u0005\u0006\u007f\n\u0006\u0003\u0007\u0003\u0007\u0003\u0007\u0003\u0007\u0007\u0007\u0085\n\u0007\f\u0007\u000e\u0007\u0088\u000b\u0007\u0003\u0007\u0005\u0007\u008b\n\u0007\u0003\b\u0003\b\u0003\b\u0003\b\u0003\b\u0005\b\u0092\n\b\u0003\t\u0003\t\u0003\n\u0003\n\u0003\n\u0003\n\u0003\n\u0003\n\u0005\n\u009c\n\n\u0003\u000b\u0003\u000b\u0003\f\u0003\f\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0007\r¨\n\r\f\r\u000e\r«\u000b\r\u0003\r\u0005\r®\n\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0007\r¹\n\r\f\r\u000e\r¼\u000b\r\u0003\u000e\u0003\u000e\u0003\u000e\u0002\u0004\u0004\u0018\u000f\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u0002\n\u0004\u0002((--\u0003\u0002(-\u0003\u0002\n\f\u0003\u0002\r\u000f\u0003\u0002$%\u0003\u0002\u0014\u0016\u0003\u0002\u001b\u001e\u0003\u0002&'\u0002Ð\u0002\u001c\u0003\u0002\u0002\u0002\u0004?\u0003\u0002\u0002\u0002\u0006[\u0003\u0002\u0002\u0002\bl\u0003\u0002\u0002\u0002\n~\u0003\u0002\u0002\u0002\f\u0080\u0003\u0002\u0002\u0002\u000e\u008c\u0003\u0002\u0002\u0002\u0010\u0093\u0003\u0002\u0002\u0002\u0012\u009b\u0003\u0002\u0002\u0002\u0014\u009d\u0003\u0002\u0002\u0002\u0016\u009f\u0003\u0002\u0002\u0002\u0018\u00ad\u0003\u0002\u0002\u0002\u001a½\u0003\u0002\u0002\u0002\u001c\u001d\u0005\u0004\u0003\u0002\u001d\u001e\u0007\u0002\u0002\u0003\u001e\u0003\u0003\u0002\u0002\u0002\u001f \b\u0003\u0001\u0002 !\u0007\u0003\u0002\u0002!\"\u0007\u0004\u0002\u0002\"#\u0005\u0018\r\u0002#$\u0007\u0005\u0002\u0002$%\u0005\u0018\r\u0002%&\u0007\u0005\u0002\u0002&'\u0005\u0018\r\u0002'(\u0007\u0006\u0002\u0002()\u0007\u0007\u0002\u0002)*\u0005\u0004\u0003\u0002*+\u0007\b\u0002\u0002+@\u0003\u0002\u0002\u0002,@\u0005\u0010\t\u0002-.\u0005\u0012\n\u0002./\t\u0002\u0002\u0002/0\u0005\b\u0005\u00020@\u0003\u0002\u0002\u000212\u0005\u0014\u000b\u000223\t\u0002\u0002\u000234\u0005\u0006\u0004\u00024@\u0003\u0002\u0002\u000256\u0005\u0016\f\u000267\t\u0003\u0002\u000278\u0005\u0018\r\u00028@\u0003\u0002\u0002\u00029:\u0007\t\u0002\u0002:@\u0005\u0004\u0003\u0006;<\u0007\u0004\u0002\u0002<=\u0005\u0004\u0003\u0002=>\u0007\u0006\u0002\u0002>@\u0003\u0002\u0002\u0002?\u001f\u0003\u0002\u0002\u0002?,\u0003\u0002\u0002\u0002?-\u0003\u0002\u0002\u0002?1\u0003\u0002\u0002\u0002?5\u0003\u0002\u0002\u0002?9\u0003\u0002\u0002\u0002?;\u0003\u0002\u0002\u0002@I\u0003\u0002\u0002\u0002AB\f\u0005\u0002\u0002BC\t\u0004\u0002\u0002CH\u0005\u0004\u0003\u0006DE\f\u0004\u0002\u0002EF\t\u0005\u0002\u0002FH\u0005\u0004\u0003\u0005GA\u0003\u0002\u0002\u0002GD\u0003\u0002\u0002\u0002HK\u0003\u0002\u0002\u0002IG\u0003\u0002\u0002\u0002IJ\u0003\u0002\u0002\u0002J\u0005\u0003\u0002\u0002\u0002KI\u0003\u0002\u0002\u0002L\\\u0005\u001a\u000e\u0002MN\u0007\u0010\u0002\u0002NS\u0005\u001a\u000e\u0002OP\u0007\u0005\u0002\u0002PR\u0005\u001a\u000e\u0002QO\u0003\u0002\u0002\u0002RU\u0003\u0002\u0002\u0002SQ\u0003\u0002\u0002\u0002ST\u0003\u0002\u0002\u0002TW\u0003\u0002\u0002\u0002US\u0003\u0002\u0002\u0002VX\u0007\u0005\u0002\u0002WV\u0003\u0002\u0002\u0002WX\u0003\u0002\u0002\u0002XY\u0003\u0002\u0002\u0002YZ\u0007\u0011\u0002\u0002Z\\\u0003\u0002\u0002\u0002[L\u0003\u0002\u0002\u0002[M\u0003\u0002\u0002\u0002\\\u0007\u0003\u0002\u0002\u0002]m\u0005\n\u0006\u0002^_\u0007\u0010\u0002\u0002_d\u0005\n\u0006\u0002`a\u0007\u0005\u0002\u0002ac\u0005\n\u0006\u0002b`\u0003\u0002\u0002\u0002cf\u0003\u0002\u0002\u0002db\u0003\u0002\u0002\u0002de\u0003\u0002\u0002\u0002eh\u0003\u0002\u0002\u0002fd\u0003\u0002\u0002\u0002gi\u0007\u0005\u0002\u0002hg\u0003\u0002\u0002\u0002hi\u0003\u0002\u0002\u0002ij\u0003\u0002\u0002\u0002jk\u0007\u0011\u0002\u0002km\u0003\u0002\u0002\u0002l]\u0003\u0002\u0002\u0002l^\u0003\u0002\u0002\u0002m\t\u0003\u0002\u0002\u0002no\u0007\u0012\u0002\u0002o\u007f\u0005\u001a\u000e\u0002p|\u0005\u001a\u000e\u0002qr\u0007\u0010\u0002\u0002rw\u0005\f\u0007\u0002st\u0007\u0005\u0002\u0002tv\u0005\f\u0007\u0002us\u0003\u0002\u0002\u0002vy\u0003\u0002\u0002\u0002wu\u0003\u0002\u0002\u0002wx\u0003\u0002\u0002\u0002xz\u0003\u0002\u0002\u0002yw\u0003\u0002\u0002\u0002z{\u0007\u0011\u0002\u0002{}\u0003\u0002\u0002\u0002|q\u0003\u0002\u0002\u0002|}\u0003\u0002\u0002\u0002}\u007f\u0003\u0002\u0002\u0002~n\u0003\u0002\u0002\u0002~p\u0003\u0002\u0002\u0002\u007f\u000b\u0003\u0002\u0002\u0002\u0080\u0081\u0005\u001a\u000e\u0002\u0081\u008a\u0007(\u0002\u0002\u0082\u008b\u0005\u001a\u000e\u0002\u0083\u0085\t\u0006\u0002\u0002\u0084\u0083\u0003\u0002\u0002\u0002\u0085\u0088\u0003\u0002\u0002\u0002\u0086\u0084\u0003\u0002\u0002\u0002\u0086\u0087\u0003\u0002\u0002\u0002\u0087\u0089\u0003\u0002\u0002\u0002\u0088\u0086\u0003\u0002\u0002\u0002\u0089\u008b\u0007\u001f\u0002\u0002\u008a\u0082\u0003\u0002\u0002\u0002\u008a\u0086\u0003\u0002\u0002\u0002\u008b\r\u0003\u0002\u0002\u0002\u008c\u0091\u0007\u0013\u0002\u0002\u008d\u008e\u0007\u0004\u0002\u0002\u008e\u008f\u0005\u0018\r\u0002\u008f\u0090\u0007\u0006\u0002\u0002\u0090\u0092\u0003\u0002\u0002\u0002\u0091\u008d\u0003\u0002\u0002\u0002\u0091\u0092\u0003\u0002\u0002\u0002\u0092\u000f\u0003\u0002\u0002\u0002\u0093\u0094\t\u0007\u0002\u0002\u0094\u0011\u0003\u0002\u0002\u0002\u0095\u009c\u0007 \u0002\u0002\u0096\u009c\u0007\u0017\u0002\u0002\u0097\u009c\u0007\u0018\u0002\u0002\u0098\u009c\u0005\u000e\b\u0002\u0099\u009c\u0007\u0019\u0002\u0002\u009a\u009c\u0007\u001a\u0002\u0002\u009b\u0095\u0003\u0002\u0002\u0002\u009b\u0096\u0003\u0002\u0002\u0002\u009b\u0097\u0003\u0002\u0002\u0002\u009b\u0098\u0003\u0002\u0002\u0002\u009b\u0099\u0003\u0002\u0002\u0002\u009b\u009a\u0003\u0002\u0002\u0002\u009c\u0013\u0003\u0002\u0002\u0002\u009d\u009e\u0007!\u0002\u0002\u009e\u0015\u0003\u0002\u0002\u0002\u009f \t\b\u0002\u0002 \u0017\u0003\u0002\u0002\u0002¡¢\b\r\u0001\u0002¢£\u0007\u0004\u0002\u0002£¤\u0005\u0018\r\u0002¤¥\u0007\u0006\u0002\u0002¥®\u0003\u0002\u0002\u0002¦¨\t\u0006\u0002\u0002§¦\u0003\u0002\u0002\u0002¨«\u0003\u0002\u0002\u0002©§\u0003\u0002\u0002\u0002©ª\u0003\u0002\u0002\u0002ª¬\u0003\u0002\u0002\u0002«©\u0003\u0002\u0002\u0002¬®\u0007\u001f\u0002\u0002\u00ad¡\u0003\u0002\u0002\u0002\u00ad©\u0003\u0002\u0002\u0002®º\u0003\u0002\u0002\u0002¯°\f\u0007\u0002\u0002°±\u0007#\u0002\u0002±¹\u0005\u0018\r\b²³\f\u0006\u0002\u0002³´\t\t\u0002\u0002´¹\u0005\u0018\r\u0007µ¶\f\u0005\u0002\u0002¶·\t\u0006\u0002\u0002·¹\u0005\u0018\r\u0006¸¯\u0003\u0002\u0002\u0002¸²\u0003\u0002\u0002\u0002¸µ\u0003\u0002\u0002\u0002¹¼\u0003\u0002\u0002\u0002º¸\u0003\u0002\u0002\u0002º»\u0003\u0002\u0002\u0002»\u0019\u0003\u0002\u0002\u0002¼º\u0003\u0002\u0002\u0002½¾\u0007\"\u0002\u0002¾\u001b\u0003\u0002\u0002\u0002\u0016?GISW[dhlw|~\u0086\u008a\u0091\u009b©\u00ad¸º";
   }

   public ATN getATN() {
      return _ATN;
   }

   public MaskParser(TokenStream input) {
      super(input);
      this._interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
   }

   public final MaskParser.MaskContext mask() throws RecognitionException {
      MaskParser.MaskContext _localctx = new MaskParser.MaskContext(this._ctx, this.getState());
      this.enterRule(_localctx, 0, 0);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(26);
         this.maskElement(0);
         this.setState(27);
         this.match(-1);
      } catch (RecognitionException var6) {
         _localctx.exception = var6;
         this._errHandler.reportError(this, var6);
         this._errHandler.recover(this, var6);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.MaskElementContext maskElement() throws RecognitionException {
      return this.maskElement(0);
   }

   private MaskParser.MaskElementContext maskElement(int _p) throws RecognitionException {
      ParserRuleContext _parentctx = this._ctx;
      int _parentState = this.getState();
      MaskParser.MaskElementContext _localctx = new MaskParser.MaskElementContext(this._ctx, _parentState);
      int _startState = 2;
      this.enterRecursionRule(_localctx, 2, 1, _p);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(61);
         this._errHandler.sync(this);
         switch (this._input.LA(1)) {
            case 1:
               _localctx = new MaskParser.MaskElementOffsetContext(_localctx);
               this._ctx = _localctx;
               this.setState(30);
               this.match(1);
               this.setState(31);
               this.match(2);
               this.setState(32);
               this.numeric(0);
               this.setState(33);
               this.match(3);
               this.setState(34);
               this.numeric(0);
               this.setState(35);
               this.match(3);
               this.setState(36);
               this.numeric(0);
               this.setState(37);
               this.match(4);
               this.setState(38);
               this.match(5);
               this.setState(39);
               this.maskElement(0);
               this.setState(40);
               this.match(6);
               break;
            case 2:
               _localctx = new MaskParser.MaskElementParenContext(_localctx);
               this._ctx = _localctx;
               this.setState(57);
               this.match(2);
               this.setState(58);
               this.maskElement(0);
               this.setState(59);
               this.match(4);
               break;
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 29:
            default:
               throw new NoViableAltException(this);
            case 7:
               _localctx = new MaskParser.MaskElementNotContext(_localctx);
               this._ctx = _localctx;
               this.setState(55);
               this.match(7);
               this.setState(56);
               this.maskElement(4);
               break;
            case 17:
            case 21:
            case 22:
            case 23:
            case 24:
            case 30:
               _localctx = new MaskParser.MaskElementCmpBlockContext(_localctx);
               this._ctx = _localctx;
               this.setState(43);
               this.cmpBlock();
               this.setState(44);
               ((MaskParser.MaskElementCmpBlockContext)_localctx).cmp = this._input.LT(1);
               int _la = this._input.LA(1);
               if (_la != 38 && _la != 43) {
                  ((MaskParser.MaskElementCmpBlockContext)_localctx).cmp = this._errHandler.recoverInline(this);
               } else {
                  if (this._input.LA(1) == -1) {
                     this.matchedEOF = true;
                  }

                  this._errHandler.reportMatch(this);
                  this.consume();
               }

               this.setState(45);
               this.multiBlockMatch();
               break;
            case 18:
            case 19:
            case 20:
               _localctx = new MaskParser.MaskElementSingleContext(_localctx);
               this._ctx = _localctx;
               this.setState(42);
               this.single();
               break;
            case 25:
            case 26:
            case 27:
            case 28:
               _localctx = new MaskParser.MaskElementCmpNumericContext(_localctx);
               this._ctx = _localctx;
               this.setState(51);
               this.cmpNumeric();
               this.setState(52);
               ((MaskParser.MaskElementCmpNumericContext)_localctx).cmp = this._input.LT(1);
               _la = this._input.LA(1);
               if ((_la & -64) == 0 && (1L << _la & 17317308137472L) != 0L) {
                  if (this._input.LA(1) == -1) {
                     this.matchedEOF = true;
                  }

                  this._errHandler.reportMatch(this);
                  this.consume();
               } else {
                  ((MaskParser.MaskElementCmpNumericContext)_localctx).cmp = this._errHandler.recoverInline(this);
               }

               this.setState(53);
               this.numeric(0);
               break;
            case 31:
               _localctx = new MaskParser.MaskElementCmpBiomeContext(_localctx);
               this._ctx = _localctx;
               this.setState(47);
               this.cmpBiome();
               this.setState(48);
               ((MaskParser.MaskElementCmpBiomeContext)_localctx).cmp = this._input.LT(1);
               _la = this._input.LA(1);
               if (_la != 38 && _la != 43) {
                  ((MaskParser.MaskElementCmpBiomeContext)_localctx).cmp = this._errHandler.recoverInline(this);
               } else {
                  if (this._input.LA(1) == -1) {
                     this.matchedEOF = true;
                  }

                  this._errHandler.reportMatch(this);
                  this.consume();
               }

               this.setState(49);
               this.multiBiomeMatch();
         }

         this._ctx.stop = this._input.LT(-1);
         this.setState(71);
         this._errHandler.sync(this);

         for (int _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 2, this._ctx);
            _alt != 2 && _alt != 0;
            _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 2, this._ctx)
         ) {
            if (_alt == 1) {
               if (this._parseListeners != null) {
                  this.triggerExitRuleEvent();
               }

               this.setState(69);
               this._errHandler.sync(this);
               switch (((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 1, this._ctx)) {
                  case 1:
                     _localctx = new MaskParser.MaskElementAndContext(new MaskParser.MaskElementContext(_parentctx, _parentState));
                     this.pushNewRecursionContext(_localctx, _startState, 1);
                     this.setState(63);
                     if (!this.precpred(this._ctx, 3)) {
                        throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                     }

                     this.setState(64);
                     int _la = this._input.LA(1);
                     if ((_la & -64) == 0 && (1L << _la & 1792L) != 0L) {
                        if (this._input.LA(1) == -1) {
                           this.matchedEOF = true;
                        }

                        this._errHandler.reportMatch(this);
                        this.consume();
                     } else {
                        this._errHandler.recoverInline(this);
                     }

                     this.setState(65);
                     this.maskElement(4);
                     break;
                  case 2:
                     _localctx = new MaskParser.MaskElementOrContext(new MaskParser.MaskElementContext(_parentctx, _parentState));
                     this.pushNewRecursionContext(_localctx, _startState, 1);
                     this.setState(66);
                     if (!this.precpred(this._ctx, 2)) {
                        throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                     }

                     this.setState(67);
                     _la = this._input.LA(1);
                     if ((_la & -64) == 0 && (1L << _la & 14336L) != 0L) {
                        if (this._input.LA(1) == -1) {
                           this.matchedEOF = true;
                        }

                        this._errHandler.reportMatch(this);
                        this.consume();
                     } else {
                        this._errHandler.recoverInline(this);
                     }

                     this.setState(68);
                     this.maskElement(3);
               }
            }

            this.setState(73);
            this._errHandler.sync(this);
         }
      } catch (RecognitionException var12) {
         _localctx.exception = var12;
         this._errHandler.reportError(this, var12);
         this._errHandler.recover(this, var12);
      } finally {
         this.unrollRecursionContexts(_parentctx);
      }

      return _localctx;
   }

   public final MaskParser.MultiBiomeMatchContext multiBiomeMatch() throws RecognitionException {
      MaskParser.MultiBiomeMatchContext _localctx = new MaskParser.MultiBiomeMatchContext(this._ctx, this.getState());
      this.enterRule(_localctx, 4, 2);

      try {
         this.setState(89);
         this._errHandler.sync(this);
         switch (this._input.LA(1)) {
            case 14:
               this.enterOuterAlt(_localctx, 2);
               this.setState(75);
               this.match(14);
               this.setState(76);
               this.identifier();
               this.setState(81);
               this._errHandler.sync(this);

               for (int _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 3, this._ctx);
                  _alt != 2 && _alt != 0;
                  _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 3, this._ctx)
               ) {
                  if (_alt == 1) {
                     this.setState(77);
                     this.match(3);
                     this.setState(78);
                     this.identifier();
                  }

                  this.setState(83);
                  this._errHandler.sync(this);
               }

               this.setState(85);
               this._errHandler.sync(this);
               int _la = this._input.LA(1);
               if (_la == 3) {
                  this.setState(84);
                  this.match(3);
               }

               this.setState(87);
               this.match(15);
               break;
            case 32:
               this.enterOuterAlt(_localctx, 1);
               this.setState(74);
               this.identifier();
               break;
            default:
               throw new NoViableAltException(this);
         }
      } catch (RecognitionException var7) {
         _localctx.exception = var7;
         this._errHandler.reportError(this, var7);
         this._errHandler.recover(this, var7);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.MultiBlockMatchContext multiBlockMatch() throws RecognitionException {
      MaskParser.MultiBlockMatchContext _localctx = new MaskParser.MultiBlockMatchContext(this._ctx, this.getState());
      this.enterRule(_localctx, 6, 3);

      try {
         this.setState(106);
         this._errHandler.sync(this);
         switch (this._input.LA(1)) {
            case 14:
               this.enterOuterAlt(_localctx, 2);
               this.setState(92);
               this.match(14);
               this.setState(93);
               this.blockMatch();
               this.setState(98);
               this._errHandler.sync(this);

               for (int _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 6, this._ctx);
                  _alt != 2 && _alt != 0;
                  _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 6, this._ctx)
               ) {
                  if (_alt == 1) {
                     this.setState(94);
                     this.match(3);
                     this.setState(95);
                     this.blockMatch();
                  }

                  this.setState(100);
                  this._errHandler.sync(this);
               }

               this.setState(102);
               this._errHandler.sync(this);
               int _la = this._input.LA(1);
               if (_la == 3) {
                  this.setState(101);
                  this.match(3);
               }

               this.setState(104);
               this.match(15);
               break;
            case 16:
            case 32:
               this.enterOuterAlt(_localctx, 1);
               this.setState(91);
               this.blockMatch();
               break;
            default:
               throw new NoViableAltException(this);
         }
      } catch (RecognitionException var7) {
         _localctx.exception = var7;
         this._errHandler.reportError(this, var7);
         this._errHandler.recover(this, var7);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.BlockMatchContext blockMatch() throws RecognitionException {
      MaskParser.BlockMatchContext _localctx = new MaskParser.BlockMatchContext(this._ctx, this.getState());
      this.enterRule(_localctx, 8, 4);

      try {
         this.setState(124);
         this._errHandler.sync(this);
         switch (this._input.LA(1)) {
            case 16:
               this.enterOuterAlt(_localctx, 1);
               this.setState(108);
               this.match(16);
               this.setState(109);
               this.identifier();
               break;
            case 32:
               this.enterOuterAlt(_localctx, 2);
               this.setState(110);
               this.identifier();
               this.setState(122);
               this._errHandler.sync(this);
               switch (((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 10, this._ctx)) {
                  case 1:
                     this.setState(111);
                     this.match(14);
                     this.setState(112);
                     this.property();
                     this.setState(117);
                     this._errHandler.sync(this);

                     for (int _la = this._input.LA(1); _la == 3; _la = this._input.LA(1)) {
                        this.setState(113);
                        this.match(3);
                        this.setState(114);
                        this.property();
                        this.setState(119);
                        this._errHandler.sync(this);
                     }

                     this.setState(120);
                     this.match(15);
                     return _localctx;
                  default:
                     return _localctx;
               }
            default:
               throw new NoViableAltException(this);
         }
      } catch (RecognitionException var7) {
         _localctx.exception = var7;
         this._errHandler.reportError(this, var7);
         this._errHandler.recover(this, var7);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.PropertyContext property() throws RecognitionException {
      MaskParser.PropertyContext _localctx = new MaskParser.PropertyContext(this._ctx, this.getState());
      this.enterRule(_localctx, 10, 5);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(126);
         this.identifier();
         this.setState(127);
         this.match(38);
         this.setState(136);
         this._errHandler.sync(this);
         switch (this._input.LA(1)) {
            case 29:
            case 34:
            case 35:
               this.setState(132);
               this._errHandler.sync(this);

               for (int _la = this._input.LA(1); _la == 34 || _la == 35; _la = this._input.LA(1)) {
                  this.setState(129);
                  _la = this._input.LA(1);
                  if (_la != 34 && _la != 35) {
                     this._errHandler.recoverInline(this);
                  } else {
                     if (this._input.LA(1) == -1) {
                        this.matchedEOF = true;
                     }

                     this._errHandler.reportMatch(this);
                     this.consume();
                  }

                  this.setState(134);
                  this._errHandler.sync(this);
               }

               this.setState(135);
               this.match(29);
               break;
            case 30:
            case 31:
            case 33:
            default:
               throw new NoViableAltException(this);
            case 32:
               this.setState(128);
               this.identifier();
         }
      } catch (RecognitionException var7) {
         _localctx.exception = var7;
         this._errHandler.reportError(this, var7);
         this._errHandler.recover(this, var7);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.NearContext near() throws RecognitionException {
      MaskParser.NearContext _localctx = new MaskParser.NearContext(this._ctx, this.getState());
      this.enterRule(_localctx, 12, 6);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(138);
         this.match(17);
         this.setState(143);
         this._errHandler.sync(this);
         int _la = this._input.LA(1);
         if (_la == 2) {
            this.setState(139);
            this.match(2);
            this.setState(140);
            this.numeric(0);
            this.setState(141);
            this.match(4);
         }
      } catch (RecognitionException var7) {
         _localctx.exception = var7;
         this._errHandler.reportError(this, var7);
         this._errHandler.recover(this, var7);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.SingleContext single() throws RecognitionException {
      MaskParser.SingleContext _localctx = new MaskParser.SingleContext(this._ctx, this.getState());
      this.enterRule(_localctx, 14, 7);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(145);
         int _la = this._input.LA(1);
         if ((_la & -64) == 0 && (1L << _la & 1835008L) != 0L) {
            if (this._input.LA(1) == -1) {
               this.matchedEOF = true;
            }

            this._errHandler.reportMatch(this);
            this.consume();
         } else {
            this._errHandler.recoverInline(this);
         }
      } catch (RecognitionException var7) {
         _localctx.exception = var7;
         this._errHandler.reportError(this, var7);
         this._errHandler.recover(this, var7);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.CmpBlockContext cmpBlock() throws RecognitionException {
      MaskParser.CmpBlockContext _localctx = new MaskParser.CmpBlockContext(this._ctx, this.getState());
      this.enterRule(_localctx, 16, 8);

      try {
         this.setState(153);
         this._errHandler.sync(this);
         switch (this._input.LA(1)) {
            case 17:
               this.enterOuterAlt(_localctx, 4);
               this.setState(150);
               this.near();
               break;
            case 18:
            case 19:
            case 20:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            default:
               throw new NoViableAltException(this);
            case 21:
               this.enterOuterAlt(_localctx, 2);
               this.setState(148);
               this.match(21);
               break;
            case 22:
               this.enterOuterAlt(_localctx, 3);
               this.setState(149);
               this.match(22);
               break;
            case 23:
               this.enterOuterAlt(_localctx, 5);
               this.setState(151);
               this.match(23);
               break;
            case 24:
               this.enterOuterAlt(_localctx, 6);
               this.setState(152);
               this.match(24);
               break;
            case 30:
               this.enterOuterAlt(_localctx, 1);
               this.setState(147);
               this.match(30);
         }
      } catch (RecognitionException var6) {
         _localctx.exception = var6;
         this._errHandler.reportError(this, var6);
         this._errHandler.recover(this, var6);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.CmpBiomeContext cmpBiome() throws RecognitionException {
      MaskParser.CmpBiomeContext _localctx = new MaskParser.CmpBiomeContext(this._ctx, this.getState());
      this.enterRule(_localctx, 18, 9);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(155);
         this.match(31);
      } catch (RecognitionException var6) {
         _localctx.exception = var6;
         this._errHandler.reportError(this, var6);
         this._errHandler.recover(this, var6);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.CmpNumericContext cmpNumeric() throws RecognitionException {
      MaskParser.CmpNumericContext _localctx = new MaskParser.CmpNumericContext(this._ctx, this.getState());
      this.enterRule(_localctx, 20, 10);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(157);
         int _la = this._input.LA(1);
         if ((_la & -64) == 0 && (1L << _la & 503316480L) != 0L) {
            if (this._input.LA(1) == -1) {
               this.matchedEOF = true;
            }

            this._errHandler.reportMatch(this);
            this.consume();
         } else {
            this._errHandler.recoverInline(this);
         }
      } catch (RecognitionException var7) {
         _localctx.exception = var7;
         this._errHandler.reportError(this, var7);
         this._errHandler.recover(this, var7);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public final MaskParser.NumericContext numeric() throws RecognitionException {
      return this.numeric(0);
   }

   private MaskParser.NumericContext numeric(int _p) throws RecognitionException {
      ParserRuleContext _parentctx = this._ctx;
      int _parentState = this.getState();
      MaskParser.NumericContext _localctx = new MaskParser.NumericContext(this._ctx, _parentState);
      int _startState = 22;
      this.enterRecursionRule(_localctx, 22, 11, _p);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(171);
         this._errHandler.sync(this);
         switch (this._input.LA(1)) {
            case 2:
               _localctx = new MaskParser.NumericParenContext(_localctx);
               this._ctx = _localctx;
               this.setState(160);
               this.match(2);
               this.setState(161);
               this.numeric(0);
               this.setState(162);
               this.match(4);
               break;
            case 29:
            case 34:
            case 35:
               _localctx = new MaskParser.NumericLiteralContext(_localctx);
               this._ctx = _localctx;
               this.setState(167);
               this._errHandler.sync(this);

               for (int _la = this._input.LA(1); _la == 34 || _la == 35; _la = this._input.LA(1)) {
                  this.setState(164);
                  _la = this._input.LA(1);
                  if (_la != 34 && _la != 35) {
                     this._errHandler.recoverInline(this);
                  } else {
                     if (this._input.LA(1) == -1) {
                        this.matchedEOF = true;
                     }

                     this._errHandler.reportMatch(this);
                     this.consume();
                  }

                  this.setState(169);
                  this._errHandler.sync(this);
               }

               this.setState(170);
               this.match(29);
               break;
            default:
               throw new NoViableAltException(this);
         }

         this._ctx.stop = this._input.LT(-1);
         this.setState(184);
         this._errHandler.sync(this);

         for (int _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 19, this._ctx);
            _alt != 2 && _alt != 0;
            _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 19, this._ctx)
         ) {
            if (_alt == 1) {
               if (this._parseListeners != null) {
                  this.triggerExitRuleEvent();
               }

               this.setState(182);
               this._errHandler.sync(this);
               switch (((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 18, this._ctx)) {
                  case 1:
                     _localctx = new MaskParser.NumericPowContext(new MaskParser.NumericContext(_parentctx, _parentState));
                     this.pushNewRecursionContext(_localctx, _startState, 11);
                     this.setState(173);
                     if (!this.precpred(this._ctx, 5)) {
                        throw new FailedPredicateException(this, "precpred(_ctx, 5)");
                     }

                     this.setState(174);
                     this.match(33);
                     this.setState(175);
                     this.numeric(6);
                     break;
                  case 2:
                     _localctx = new MaskParser.NumericMultOrDivContext(new MaskParser.NumericContext(_parentctx, _parentState));
                     this.pushNewRecursionContext(_localctx, _startState, 11);
                     this.setState(176);
                     if (!this.precpred(this._ctx, 4)) {
                        throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                     }

                     this.setState(177);
                     ((MaskParser.NumericMultOrDivContext)_localctx).op = this._input.LT(1);
                     int _la = this._input.LA(1);
                     if (_la != 36 && _la != 37) {
                        ((MaskParser.NumericMultOrDivContext)_localctx).op = this._errHandler.recoverInline(this);
                     } else {
                        if (this._input.LA(1) == -1) {
                           this.matchedEOF = true;
                        }

                        this._errHandler.reportMatch(this);
                        this.consume();
                     }

                     this.setState(178);
                     this.numeric(5);
                     break;
                  case 3:
                     _localctx = new MaskParser.NumericAddOrSubtractContext(new MaskParser.NumericContext(_parentctx, _parentState));
                     this.pushNewRecursionContext(_localctx, _startState, 11);
                     this.setState(179);
                     if (!this.precpred(this._ctx, 3)) {
                        throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                     }

                     this.setState(180);
                     ((MaskParser.NumericAddOrSubtractContext)_localctx).op = this._input.LT(1);
                     _la = this._input.LA(1);
                     if (_la != 34 && _la != 35) {
                        ((MaskParser.NumericAddOrSubtractContext)_localctx).op = this._errHandler.recoverInline(this);
                     } else {
                        if (this._input.LA(1) == -1) {
                           this.matchedEOF = true;
                        }

                        this._errHandler.reportMatch(this);
                        this.consume();
                     }

                     this.setState(181);
                     this.numeric(4);
               }
            }

            this.setState(186);
            this._errHandler.sync(this);
         }
      } catch (RecognitionException var12) {
         _localctx.exception = var12;
         this._errHandler.reportError(this, var12);
         this._errHandler.recover(this, var12);
      } finally {
         this.unrollRecursionContexts(_parentctx);
      }

      return _localctx;
   }

   public final MaskParser.IdentifierContext identifier() throws RecognitionException {
      MaskParser.IdentifierContext _localctx = new MaskParser.IdentifierContext(this._ctx, this.getState());
      this.enterRule(_localctx, 24, 12);

      try {
         this.enterOuterAlt(_localctx, 1);
         this.setState(187);
         this.match(32);
      } catch (RecognitionException var6) {
         _localctx.exception = var6;
         this._errHandler.reportError(this, var6);
         this._errHandler.recover(this, var6);
      } finally {
         this.exitRule();
      }

      return _localctx;
   }

   public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
      switch (ruleIndex) {
         case 1:
            return this.maskElement_sempred((MaskParser.MaskElementContext)_localctx, predIndex);
         case 11:
            return this.numeric_sempred((MaskParser.NumericContext)_localctx, predIndex);
         default:
            return true;
      }
   }

   private boolean maskElement_sempred(MaskParser.MaskElementContext _localctx, int predIndex) {
      switch (predIndex) {
         case 0:
            return this.precpred(this._ctx, 3);
         case 1:
            return this.precpred(this._ctx, 2);
         default:
            return true;
      }
   }

   private boolean numeric_sempred(MaskParser.NumericContext _localctx, int predIndex) {
      switch (predIndex) {
         case 2:
            return this.precpred(this._ctx, 5);
         case 3:
            return this.precpred(this._ctx, 4);
         case 4:
            return this.precpred(this._ctx, 3);
         default:
            return true;
      }
   }

   static {
      RuntimeMetaData.checkVersion("4.7.1", "4.7.1");

      for (int i = 0; i < tokenNames.length; i++) {
         tokenNames[i] = VOCABULARY.getLiteralName(i);
         if (tokenNames[i] == null) {
            tokenNames[i] = VOCABULARY.getSymbolicName(i);
         }

         if (tokenNames[i] == null) {
            tokenNames[i] = "<INVALID>";
         }
      }

      _ATN = new ATNDeserializer()
         .deserialize(
            "\u0003悋Ꜫ脳맭䅼㯧瞆奤\u0003.À\u0004\u0002\t\u0002\u0004\u0003\t\u0003\u0004\u0004\t\u0004\u0004\u0005\t\u0005\u0004\u0006\t\u0006\u0004\u0007\t\u0007\u0004\b\t\b\u0004\t\t\t\u0004\n\t\n\u0004\u000b\t\u000b\u0004\f\t\f\u0004\r\t\r\u0004\u000e\t\u000e\u0003\u0002\u0003\u0002\u0003\u0002\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0005\u0003@\n\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0003\u0007\u0003H\n\u0003\f\u0003\u000e\u0003K\u000b\u0003\u0003\u0004\u0003\u0004\u0003\u0004\u0003\u0004\u0003\u0004\u0007\u0004R\n\u0004\f\u0004\u000e\u0004U\u000b\u0004\u0003\u0004\u0005\u0004X\n\u0004\u0003\u0004\u0003\u0004\u0005\u0004\\\n\u0004\u0003\u0005\u0003\u0005\u0003\u0005\u0003\u0005\u0003\u0005\u0007\u0005c\n\u0005\f\u0005\u000e\u0005f\u000b\u0005\u0003\u0005\u0005\u0005i\n\u0005\u0003\u0005\u0003\u0005\u0005\u0005m\n\u0005\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0003\u0006\u0007\u0006v\n\u0006\f\u0006\u000e\u0006y\u000b\u0006\u0003\u0006\u0003\u0006\u0005\u0006}\n\u0006\u0005\u0006\u007f\n\u0006\u0003\u0007\u0003\u0007\u0003\u0007\u0003\u0007\u0007\u0007\u0085\n\u0007\f\u0007\u000e\u0007\u0088\u000b\u0007\u0003\u0007\u0005\u0007\u008b\n\u0007\u0003\b\u0003\b\u0003\b\u0003\b\u0003\b\u0005\b\u0092\n\b\u0003\t\u0003\t\u0003\n\u0003\n\u0003\n\u0003\n\u0003\n\u0003\n\u0005\n\u009c\n\n\u0003\u000b\u0003\u000b\u0003\f\u0003\f\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0007\r¨\n\r\f\r\u000e\r«\u000b\r\u0003\r\u0005\r®\n\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0003\r\u0007\r¹\n\r\f\r\u000e\r¼\u000b\r\u0003\u000e\u0003\u000e\u0003\u000e\u0002\u0004\u0004\u0018\u000f\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u0002\n\u0004\u0002((--\u0003\u0002(-\u0003\u0002\n\f\u0003\u0002\r\u000f\u0003\u0002$%\u0003\u0002\u0014\u0016\u0003\u0002\u001b\u001e\u0003\u0002&'\u0002Ð\u0002\u001c\u0003\u0002\u0002\u0002\u0004?\u0003\u0002\u0002\u0002\u0006[\u0003\u0002\u0002\u0002\bl\u0003\u0002\u0002\u0002\n~\u0003\u0002\u0002\u0002\f\u0080\u0003\u0002\u0002\u0002\u000e\u008c\u0003\u0002\u0002\u0002\u0010\u0093\u0003\u0002\u0002\u0002\u0012\u009b\u0003\u0002\u0002\u0002\u0014\u009d\u0003\u0002\u0002\u0002\u0016\u009f\u0003\u0002\u0002\u0002\u0018\u00ad\u0003\u0002\u0002\u0002\u001a½\u0003\u0002\u0002\u0002\u001c\u001d\u0005\u0004\u0003\u0002\u001d\u001e\u0007\u0002\u0002\u0003\u001e\u0003\u0003\u0002\u0002\u0002\u001f \b\u0003\u0001\u0002 !\u0007\u0003\u0002\u0002!\"\u0007\u0004\u0002\u0002\"#\u0005\u0018\r\u0002#$\u0007\u0005\u0002\u0002$%\u0005\u0018\r\u0002%&\u0007\u0005\u0002\u0002&'\u0005\u0018\r\u0002'(\u0007\u0006\u0002\u0002()\u0007\u0007\u0002\u0002)*\u0005\u0004\u0003\u0002*+\u0007\b\u0002\u0002+@\u0003\u0002\u0002\u0002,@\u0005\u0010\t\u0002-.\u0005\u0012\n\u0002./\t\u0002\u0002\u0002/0\u0005\b\u0005\u00020@\u0003\u0002\u0002\u000212\u0005\u0014\u000b\u000223\t\u0002\u0002\u000234\u0005\u0006\u0004\u00024@\u0003\u0002\u0002\u000256\u0005\u0016\f\u000267\t\u0003\u0002\u000278\u0005\u0018\r\u00028@\u0003\u0002\u0002\u00029:\u0007\t\u0002\u0002:@\u0005\u0004\u0003\u0006;<\u0007\u0004\u0002\u0002<=\u0005\u0004\u0003\u0002=>\u0007\u0006\u0002\u0002>@\u0003\u0002\u0002\u0002?\u001f\u0003\u0002\u0002\u0002?,\u0003\u0002\u0002\u0002?-\u0003\u0002\u0002\u0002?1\u0003\u0002\u0002\u0002?5\u0003\u0002\u0002\u0002?9\u0003\u0002\u0002\u0002?;\u0003\u0002\u0002\u0002@I\u0003\u0002\u0002\u0002AB\f\u0005\u0002\u0002BC\t\u0004\u0002\u0002CH\u0005\u0004\u0003\u0006DE\f\u0004\u0002\u0002EF\t\u0005\u0002\u0002FH\u0005\u0004\u0003\u0005GA\u0003\u0002\u0002\u0002GD\u0003\u0002\u0002\u0002HK\u0003\u0002\u0002\u0002IG\u0003\u0002\u0002\u0002IJ\u0003\u0002\u0002\u0002J\u0005\u0003\u0002\u0002\u0002KI\u0003\u0002\u0002\u0002L\\\u0005\u001a\u000e\u0002MN\u0007\u0010\u0002\u0002NS\u0005\u001a\u000e\u0002OP\u0007\u0005\u0002\u0002PR\u0005\u001a\u000e\u0002QO\u0003\u0002\u0002\u0002RU\u0003\u0002\u0002\u0002SQ\u0003\u0002\u0002\u0002ST\u0003\u0002\u0002\u0002TW\u0003\u0002\u0002\u0002US\u0003\u0002\u0002\u0002VX\u0007\u0005\u0002\u0002WV\u0003\u0002\u0002\u0002WX\u0003\u0002\u0002\u0002XY\u0003\u0002\u0002\u0002YZ\u0007\u0011\u0002\u0002Z\\\u0003\u0002\u0002\u0002[L\u0003\u0002\u0002\u0002[M\u0003\u0002\u0002\u0002\\\u0007\u0003\u0002\u0002\u0002]m\u0005\n\u0006\u0002^_\u0007\u0010\u0002\u0002_d\u0005\n\u0006\u0002`a\u0007\u0005\u0002\u0002ac\u0005\n\u0006\u0002b`\u0003\u0002\u0002\u0002cf\u0003\u0002\u0002\u0002db\u0003\u0002\u0002\u0002de\u0003\u0002\u0002\u0002eh\u0003\u0002\u0002\u0002fd\u0003\u0002\u0002\u0002gi\u0007\u0005\u0002\u0002hg\u0003\u0002\u0002\u0002hi\u0003\u0002\u0002\u0002ij\u0003\u0002\u0002\u0002jk\u0007\u0011\u0002\u0002km\u0003\u0002\u0002\u0002l]\u0003\u0002\u0002\u0002l^\u0003\u0002\u0002\u0002m\t\u0003\u0002\u0002\u0002no\u0007\u0012\u0002\u0002o\u007f\u0005\u001a\u000e\u0002p|\u0005\u001a\u000e\u0002qr\u0007\u0010\u0002\u0002rw\u0005\f\u0007\u0002st\u0007\u0005\u0002\u0002tv\u0005\f\u0007\u0002us\u0003\u0002\u0002\u0002vy\u0003\u0002\u0002\u0002wu\u0003\u0002\u0002\u0002wx\u0003\u0002\u0002\u0002xz\u0003\u0002\u0002\u0002yw\u0003\u0002\u0002\u0002z{\u0007\u0011\u0002\u0002{}\u0003\u0002\u0002\u0002|q\u0003\u0002\u0002\u0002|}\u0003\u0002\u0002\u0002}\u007f\u0003\u0002\u0002\u0002~n\u0003\u0002\u0002\u0002~p\u0003\u0002\u0002\u0002\u007f\u000b\u0003\u0002\u0002\u0002\u0080\u0081\u0005\u001a\u000e\u0002\u0081\u008a\u0007(\u0002\u0002\u0082\u008b\u0005\u001a\u000e\u0002\u0083\u0085\t\u0006\u0002\u0002\u0084\u0083\u0003\u0002\u0002\u0002\u0085\u0088\u0003\u0002\u0002\u0002\u0086\u0084\u0003\u0002\u0002\u0002\u0086\u0087\u0003\u0002\u0002\u0002\u0087\u0089\u0003\u0002\u0002\u0002\u0088\u0086\u0003\u0002\u0002\u0002\u0089\u008b\u0007\u001f\u0002\u0002\u008a\u0082\u0003\u0002\u0002\u0002\u008a\u0086\u0003\u0002\u0002\u0002\u008b\r\u0003\u0002\u0002\u0002\u008c\u0091\u0007\u0013\u0002\u0002\u008d\u008e\u0007\u0004\u0002\u0002\u008e\u008f\u0005\u0018\r\u0002\u008f\u0090\u0007\u0006\u0002\u0002\u0090\u0092\u0003\u0002\u0002\u0002\u0091\u008d\u0003\u0002\u0002\u0002\u0091\u0092\u0003\u0002\u0002\u0002\u0092\u000f\u0003\u0002\u0002\u0002\u0093\u0094\t\u0007\u0002\u0002\u0094\u0011\u0003\u0002\u0002\u0002\u0095\u009c\u0007 \u0002\u0002\u0096\u009c\u0007\u0017\u0002\u0002\u0097\u009c\u0007\u0018\u0002\u0002\u0098\u009c\u0005\u000e\b\u0002\u0099\u009c\u0007\u0019\u0002\u0002\u009a\u009c\u0007\u001a\u0002\u0002\u009b\u0095\u0003\u0002\u0002\u0002\u009b\u0096\u0003\u0002\u0002\u0002\u009b\u0097\u0003\u0002\u0002\u0002\u009b\u0098\u0003\u0002\u0002\u0002\u009b\u0099\u0003\u0002\u0002\u0002\u009b\u009a\u0003\u0002\u0002\u0002\u009c\u0013\u0003\u0002\u0002\u0002\u009d\u009e\u0007!\u0002\u0002\u009e\u0015\u0003\u0002\u0002\u0002\u009f \t\b\u0002\u0002 \u0017\u0003\u0002\u0002\u0002¡¢\b\r\u0001\u0002¢£\u0007\u0004\u0002\u0002£¤\u0005\u0018\r\u0002¤¥\u0007\u0006\u0002\u0002¥®\u0003\u0002\u0002\u0002¦¨\t\u0006\u0002\u0002§¦\u0003\u0002\u0002\u0002¨«\u0003\u0002\u0002\u0002©§\u0003\u0002\u0002\u0002©ª\u0003\u0002\u0002\u0002ª¬\u0003\u0002\u0002\u0002«©\u0003\u0002\u0002\u0002¬®\u0007\u001f\u0002\u0002\u00ad¡\u0003\u0002\u0002\u0002\u00ad©\u0003\u0002\u0002\u0002®º\u0003\u0002\u0002\u0002¯°\f\u0007\u0002\u0002°±\u0007#\u0002\u0002±¹\u0005\u0018\r\b²³\f\u0006\u0002\u0002³´\t\t\u0002\u0002´¹\u0005\u0018\r\u0007µ¶\f\u0005\u0002\u0002¶·\t\u0006\u0002\u0002·¹\u0005\u0018\r\u0006¸¯\u0003\u0002\u0002\u0002¸²\u0003\u0002\u0002\u0002¸µ\u0003\u0002\u0002\u0002¹¼\u0003\u0002\u0002\u0002º¸\u0003\u0002\u0002\u0002º»\u0003\u0002\u0002\u0002»\u0019\u0003\u0002\u0002\u0002¼º\u0003\u0002\u0002\u0002½¾\u0007\"\u0002\u0002¾\u001b\u0003\u0002\u0002\u0002\u0016?GISW[dhlw|~\u0086\u008a\u0091\u009b©\u00ad¸º"
               .toCharArray()
         );
      _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];

      for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
         _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
      }
   }

   public static class BlockMatchContext extends ParserRuleContext {
      public MaskParser.IdentifierContext identifier() {
         return (MaskParser.IdentifierContext)this.getRuleContext(MaskParser.IdentifierContext.class, 0);
      }

      public List<MaskParser.PropertyContext> property() {
         return this.getRuleContexts(MaskParser.PropertyContext.class);
      }

      public MaskParser.PropertyContext property(int i) {
         return (MaskParser.PropertyContext)this.getRuleContext(MaskParser.PropertyContext.class, i);
      }

      public BlockMatchContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 4;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterBlockMatch(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitBlockMatch(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitBlockMatch(this) : visitor.visitChildren(this));
      }
   }

   public static class CmpBiomeContext extends ParserRuleContext {
      public TerminalNode BIOME() {
         return this.getToken(31, 0);
      }

      public CmpBiomeContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 9;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterCmpBiome(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitCmpBiome(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitCmpBiome(this) : visitor.visitChildren(this));
      }
   }

   public static class CmpBlockContext extends ParserRuleContext {
      public TerminalNode BLOCK() {
         return this.getToken(30, 0);
      }

      public MaskParser.NearContext near() {
         return (MaskParser.NearContext)this.getRuleContext(MaskParser.NearContext.class, 0);
      }

      public CmpBlockContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 8;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterCmpBlock(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitCmpBlock(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitCmpBlock(this) : visitor.visitChildren(this));
      }
   }

   public static class CmpNumericContext extends ParserRuleContext {
      public CmpNumericContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 10;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterCmpNumeric(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitCmpNumeric(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitCmpNumeric(this) : visitor.visitChildren(this));
      }
   }

   public static class IdentifierContext extends ParserRuleContext {
      public TerminalNode IDENTIFIER_FRAGMENT() {
         return this.getToken(32, 0);
      }

      public IdentifierContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 12;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterIdentifier(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitIdentifier(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitIdentifier(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskContext extends ParserRuleContext {
      public MaskParser.MaskElementContext maskElement() {
         return (MaskParser.MaskElementContext)this.getRuleContext(MaskParser.MaskElementContext.class, 0);
      }

      public TerminalNode EOF() {
         return this.getToken(-1, 0);
      }

      public MaskContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 0;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMask(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMask(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMask(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementAndContext extends MaskParser.MaskElementContext {
      public List<MaskParser.MaskElementContext> maskElement() {
         return this.getRuleContexts(MaskParser.MaskElementContext.class);
      }

      public MaskParser.MaskElementContext maskElement(int i) {
         return (MaskParser.MaskElementContext)this.getRuleContext(MaskParser.MaskElementContext.class, i);
      }

      public MaskElementAndContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementAnd(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementAnd(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementAnd(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementCmpBiomeContext extends MaskParser.MaskElementContext {
      public Token cmp;

      public MaskParser.CmpBiomeContext cmpBiome() {
         return (MaskParser.CmpBiomeContext)this.getRuleContext(MaskParser.CmpBiomeContext.class, 0);
      }

      public MaskParser.MultiBiomeMatchContext multiBiomeMatch() {
         return (MaskParser.MultiBiomeMatchContext)this.getRuleContext(MaskParser.MultiBiomeMatchContext.class, 0);
      }

      public TerminalNode EQUALS() {
         return this.getToken(38, 0);
      }

      public TerminalNode NOT_EQUALS() {
         return this.getToken(43, 0);
      }

      public MaskElementCmpBiomeContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementCmpBiome(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementCmpBiome(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementCmpBiome(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementCmpBlockContext extends MaskParser.MaskElementContext {
      public Token cmp;

      public MaskParser.CmpBlockContext cmpBlock() {
         return (MaskParser.CmpBlockContext)this.getRuleContext(MaskParser.CmpBlockContext.class, 0);
      }

      public MaskParser.MultiBlockMatchContext multiBlockMatch() {
         return (MaskParser.MultiBlockMatchContext)this.getRuleContext(MaskParser.MultiBlockMatchContext.class, 0);
      }

      public TerminalNode EQUALS() {
         return this.getToken(38, 0);
      }

      public TerminalNode NOT_EQUALS() {
         return this.getToken(43, 0);
      }

      public MaskElementCmpBlockContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementCmpBlock(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementCmpBlock(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementCmpBlock(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementCmpNumericContext extends MaskParser.MaskElementContext {
      public Token cmp;

      public MaskParser.CmpNumericContext cmpNumeric() {
         return (MaskParser.CmpNumericContext)this.getRuleContext(MaskParser.CmpNumericContext.class, 0);
      }

      public MaskParser.NumericContext numeric() {
         return (MaskParser.NumericContext)this.getRuleContext(MaskParser.NumericContext.class, 0);
      }

      public TerminalNode EQUALS() {
         return this.getToken(38, 0);
      }

      public TerminalNode NOT_EQUALS() {
         return this.getToken(43, 0);
      }

      public TerminalNode LT() {
         return this.getToken(39, 0);
      }

      public TerminalNode LT_EQ() {
         return this.getToken(40, 0);
      }

      public TerminalNode GT() {
         return this.getToken(41, 0);
      }

      public TerminalNode GT_EQ() {
         return this.getToken(42, 0);
      }

      public MaskElementCmpNumericContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementCmpNumeric(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementCmpNumeric(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementCmpNumeric(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementContext extends ParserRuleContext {
      public MaskElementContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 1;
      }

      public MaskElementContext() {
      }

      public void copyFrom(MaskParser.MaskElementContext ctx) {
         super.copyFrom(ctx);
      }
   }

   public static class MaskElementNotContext extends MaskParser.MaskElementContext {
      public MaskParser.MaskElementContext maskElement() {
         return (MaskParser.MaskElementContext)this.getRuleContext(MaskParser.MaskElementContext.class, 0);
      }

      public MaskElementNotContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementNot(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementNot(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementNot(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementOffsetContext extends MaskParser.MaskElementContext {
      public List<MaskParser.NumericContext> numeric() {
         return this.getRuleContexts(MaskParser.NumericContext.class);
      }

      public MaskParser.NumericContext numeric(int i) {
         return (MaskParser.NumericContext)this.getRuleContext(MaskParser.NumericContext.class, i);
      }

      public MaskParser.MaskElementContext maskElement() {
         return (MaskParser.MaskElementContext)this.getRuleContext(MaskParser.MaskElementContext.class, 0);
      }

      public MaskElementOffsetContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementOffset(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementOffset(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementOffset(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementOrContext extends MaskParser.MaskElementContext {
      public List<MaskParser.MaskElementContext> maskElement() {
         return this.getRuleContexts(MaskParser.MaskElementContext.class);
      }

      public MaskParser.MaskElementContext maskElement(int i) {
         return (MaskParser.MaskElementContext)this.getRuleContext(MaskParser.MaskElementContext.class, i);
      }

      public MaskElementOrContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementOr(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementOr(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementOr(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementParenContext extends MaskParser.MaskElementContext {
      public MaskParser.MaskElementContext maskElement() {
         return (MaskParser.MaskElementContext)this.getRuleContext(MaskParser.MaskElementContext.class, 0);
      }

      public MaskElementParenContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementParen(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementParen(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementParen(this) : visitor.visitChildren(this));
      }
   }

   public static class MaskElementSingleContext extends MaskParser.MaskElementContext {
      public MaskParser.SingleContext single() {
         return (MaskParser.SingleContext)this.getRuleContext(MaskParser.SingleContext.class, 0);
      }

      public MaskElementSingleContext(MaskParser.MaskElementContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMaskElementSingle(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMaskElementSingle(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMaskElementSingle(this) : visitor.visitChildren(this));
      }
   }

   public static class MultiBiomeMatchContext extends ParserRuleContext {
      public List<MaskParser.IdentifierContext> identifier() {
         return this.getRuleContexts(MaskParser.IdentifierContext.class);
      }

      public MaskParser.IdentifierContext identifier(int i) {
         return (MaskParser.IdentifierContext)this.getRuleContext(MaskParser.IdentifierContext.class, i);
      }

      public MultiBiomeMatchContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 2;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMultiBiomeMatch(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMultiBiomeMatch(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMultiBiomeMatch(this) : visitor.visitChildren(this));
      }
   }

   public static class MultiBlockMatchContext extends ParserRuleContext {
      public List<MaskParser.BlockMatchContext> blockMatch() {
         return this.getRuleContexts(MaskParser.BlockMatchContext.class);
      }

      public MaskParser.BlockMatchContext blockMatch(int i) {
         return (MaskParser.BlockMatchContext)this.getRuleContext(MaskParser.BlockMatchContext.class, i);
      }

      public MultiBlockMatchContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 3;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterMultiBlockMatch(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitMultiBlockMatch(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitMultiBlockMatch(this) : visitor.visitChildren(this));
      }
   }

   public static class NearContext extends ParserRuleContext {
      public MaskParser.NumericContext numeric() {
         return (MaskParser.NumericContext)this.getRuleContext(MaskParser.NumericContext.class, 0);
      }

      public NearContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 6;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterNear(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitNear(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitNear(this) : visitor.visitChildren(this));
      }
   }

   public static class NumericAddOrSubtractContext extends MaskParser.NumericContext {
      public Token op;

      public List<MaskParser.NumericContext> numeric() {
         return this.getRuleContexts(MaskParser.NumericContext.class);
      }

      public MaskParser.NumericContext numeric(int i) {
         return (MaskParser.NumericContext)this.getRuleContext(MaskParser.NumericContext.class, i);
      }

      public TerminalNode PLUS() {
         return this.getToken(34, 0);
      }

      public TerminalNode MINUS() {
         return this.getToken(35, 0);
      }

      public NumericAddOrSubtractContext(MaskParser.NumericContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterNumericAddOrSubtract(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitNumericAddOrSubtract(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitNumericAddOrSubtract(this) : visitor.visitChildren(this));
      }
   }

   public static class NumericContext extends ParserRuleContext {
      public NumericContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 11;
      }

      public NumericContext() {
      }

      public void copyFrom(MaskParser.NumericContext ctx) {
         super.copyFrom(ctx);
      }
   }

   public static class NumericLiteralContext extends MaskParser.NumericContext {
      public TerminalNode UNSIGNED_INTEGER() {
         return this.getToken(29, 0);
      }

      public List<TerminalNode> PLUS() {
         return this.getTokens(34);
      }

      public TerminalNode PLUS(int i) {
         return this.getToken(34, i);
      }

      public List<TerminalNode> MINUS() {
         return this.getTokens(35);
      }

      public TerminalNode MINUS(int i) {
         return this.getToken(35, i);
      }

      public NumericLiteralContext(MaskParser.NumericContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterNumericLiteral(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitNumericLiteral(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitNumericLiteral(this) : visitor.visitChildren(this));
      }
   }

   public static class NumericMultOrDivContext extends MaskParser.NumericContext {
      public Token op;

      public List<MaskParser.NumericContext> numeric() {
         return this.getRuleContexts(MaskParser.NumericContext.class);
      }

      public MaskParser.NumericContext numeric(int i) {
         return (MaskParser.NumericContext)this.getRuleContext(MaskParser.NumericContext.class, i);
      }

      public TerminalNode MULTIPLY() {
         return this.getToken(36, 0);
      }

      public TerminalNode DIVIDE() {
         return this.getToken(37, 0);
      }

      public NumericMultOrDivContext(MaskParser.NumericContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterNumericMultOrDiv(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitNumericMultOrDiv(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitNumericMultOrDiv(this) : visitor.visitChildren(this));
      }
   }

   public static class NumericParenContext extends MaskParser.NumericContext {
      public MaskParser.NumericContext numeric() {
         return (MaskParser.NumericContext)this.getRuleContext(MaskParser.NumericContext.class, 0);
      }

      public NumericParenContext(MaskParser.NumericContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterNumericParen(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitNumericParen(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitNumericParen(this) : visitor.visitChildren(this));
      }
   }

   public static class NumericPowContext extends MaskParser.NumericContext {
      public List<MaskParser.NumericContext> numeric() {
         return this.getRuleContexts(MaskParser.NumericContext.class);
      }

      public MaskParser.NumericContext numeric(int i) {
         return (MaskParser.NumericContext)this.getRuleContext(MaskParser.NumericContext.class, i);
      }

      public TerminalNode POW() {
         return this.getToken(33, 0);
      }

      public NumericPowContext(MaskParser.NumericContext ctx) {
         this.copyFrom(ctx);
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterNumericPow(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitNumericPow(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitNumericPow(this) : visitor.visitChildren(this));
      }
   }

   public static class PropertyContext extends ParserRuleContext {
      public List<MaskParser.IdentifierContext> identifier() {
         return this.getRuleContexts(MaskParser.IdentifierContext.class);
      }

      public MaskParser.IdentifierContext identifier(int i) {
         return (MaskParser.IdentifierContext)this.getRuleContext(MaskParser.IdentifierContext.class, i);
      }

      public TerminalNode EQUALS() {
         return this.getToken(38, 0);
      }

      public TerminalNode UNSIGNED_INTEGER() {
         return this.getToken(29, 0);
      }

      public List<TerminalNode> PLUS() {
         return this.getTokens(34);
      }

      public TerminalNode PLUS(int i) {
         return this.getToken(34, i);
      }

      public List<TerminalNode> MINUS() {
         return this.getTokens(35);
      }

      public TerminalNode MINUS(int i) {
         return this.getToken(35, i);
      }

      public PropertyContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 5;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterProperty(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitProperty(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitProperty(this) : visitor.visitChildren(this));
      }
   }

   public static class SingleContext extends ParserRuleContext {
      public SingleContext(ParserRuleContext parent, int invokingState) {
         super(parent, invokingState);
      }

      public int getRuleIndex() {
         return 7;
      }

      public void enterRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).enterSingle(this);
         }
      }

      public void exitRule(ParseTreeListener listener) {
         if (listener instanceof MaskListener) {
            ((MaskListener)listener).exitSingle(this);
         }
      }

      public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
         return (T)(visitor instanceof MaskVisitor ? ((MaskVisitor)visitor).visitSingle(this) : visitor.visitChildren(this));
      }
   }
}
