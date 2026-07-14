package com.moulberry.axiom.mask;

import com.mojang.datafixers.util.Pair;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.antlr.MaskBaseVisitor;
import com.moulberry.axiom.mask.antlr.MaskLexer;
import com.moulberry.axiom.mask.antlr.MaskParser;
import com.moulberry.axiom.mask.elements.AllMaskElement;
import com.moulberry.axiom.mask.elements.AngleMaskElement;
import com.moulberry.axiom.mask.elements.AnyMaskElement;
import com.moulberry.axiom.mask.elements.BiomeConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockAboveConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockAdjacentConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockBelowConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockNearConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockNeighborConditionMaskElement;
import com.moulberry.axiom.mask.elements.BothMaskElement;
import com.moulberry.axiom.mask.elements.CanSeeSkyMaskElement;
import com.moulberry.axiom.mask.elements.CoordMaskElement;
import com.moulberry.axiom.mask.elements.EitherMaskElement;
import com.moulberry.axiom.mask.elements.GenericBlockConditionMaskElement;
import com.moulberry.axiom.mask.elements.GenericSingleMaskElement;
import com.moulberry.axiom.mask.elements.NotMaskElement;
import com.moulberry.axiom.mask.elements.OffsetMaskElement;
import com.moulberry.axiom.mask.elements.SelectedMaskElement;
import com.moulberry.axiom.mask.elements.SurfaceMaskElement;
import com.moulberry.axiom.utils.BooleanWrapper;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import com.moulberry.axiom.utils.ClientTagsHelper;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.antlr.axiom.v4.runtime.ANTLRErrorListener;
import org.antlr.axiom.v4.runtime.CharStreams;
import org.antlr.axiom.v4.runtime.CommonTokenStream;
import org.antlr.axiom.v4.runtime.DefaultErrorStrategy;
import org.antlr.axiom.v4.runtime.InputMismatchException;
import org.antlr.axiom.v4.runtime.Parser;
import org.antlr.axiom.v4.runtime.ParserRuleContext;
import org.antlr.axiom.v4.runtime.RecognitionException;
import org.antlr.axiom.v4.runtime.Recognizer;
import org.antlr.axiom.v4.runtime.Token;
import org.antlr.axiom.v4.runtime.atn.ATNConfigSet;
import org.antlr.axiom.v4.runtime.dfa.DFA;
import org.antlr.axiom.v4.runtime.misc.IntervalSet;

public class CmdStringConverter {
   public static MaskElement fromCmdString(String string) throws CmdStringConverter.CmdStringParseException {
      string = string.toLowerCase(Locale.ROOT);
      MaskLexer lexer = new MaskLexer(CharStreams.fromString(string));
      lexer.removeErrorListeners();
      MaskParser parser = new MaskParser(new CommonTokenStream(lexer));
      MaskBaseVisitor<MaskElement> visitor = new MaskBaseVisitor<MaskElement>() {
         public MaskElement visitMask(MaskParser.MaskContext ctx) {
            return (MaskElement)ctx.maskElement().accept(this);
         }

         public MaskElement visitMaskElementNot(MaskParser.MaskElementNotContext ctx) {
            MaskElement inner = (MaskElement)ctx.maskElement().accept(this);
            return inner == null ? null : new NotMaskElement(inner);
         }

         public MaskElement visitMaskElementParen(MaskParser.MaskElementParenContext ctx) {
            return (MaskElement)ctx.maskElement().accept(this);
         }

         public MaskElement visitMaskElementSingle(MaskParser.MaskElementSingleContext ctx) {
            String var2 = ctx.single().getText();

            return (MaskElement)(switch (var2) {
               case "selected" -> new SelectedMaskElement();
               case "sky" -> new CanSeeSkyMaskElement();
               case "surface" -> new SurfaceMaskElement();
               default -> throw new FaultyImplementationError();
            });
         }

         public MaskElement visitMaskElementOr(MaskParser.MaskElementOrContext ctx) {
            MaskElement left = (MaskElement)ctx.maskElement(0).accept(this);
            MaskElement right = (MaskElement)ctx.maskElement(1).accept(this);
            if (left == null) {
               return right;
            } else {
               return (MaskElement)(right == null ? left : new EitherMaskElement(left, right));
            }
         }

         public MaskElement visitMaskElementAnd(MaskParser.MaskElementAndContext ctx) {
            MaskElement left = (MaskElement)ctx.maskElement(0).accept(this);
            MaskElement right = (MaskElement)ctx.maskElement(1).accept(this);
            if (left == null) {
               return right;
            } else {
               return (MaskElement)(right == null ? left : new BothMaskElement(left, right));
            }
         }

         public MaskElement visitMaskElementCmpBiome(MaskParser.MaskElementCmpBiomeContext ctx) {
            MaskParser.CmpBiomeContext cmpBiomeContext = ctx.cmpBiome();
            MaskParser.MultiBiomeMatchContext multiBiomeMatchContext = ctx.multiBiomeMatch();
            if (cmpBiomeContext != null && multiBiomeMatchContext != null) {
               List<MaskElement> any = new ArrayList<>();

               for (MaskParser.IdentifierContext biomeMatch : multiBiomeMatchContext.identifier()) {
                  Holder<Biome> biome = CmdStringConverter.parseBiome(biomeMatch.getText(), biomeMatch.start.getStartIndex());
                  any.add(new BiomeConditionMaskElement((ResourceKey<Biome>)biome.unwrapKey().get()));
               }

               if (ctx.cmp.getType() == 38) {
                  return new AnyMaskElement(any.toArray(new MaskElement[0]));
               } else if (ctx.cmp.getType() == 43) {
                  return new NotMaskElement(new AnyMaskElement(any.toArray(new MaskElement[0])));
               } else {
                  throw new FaultyImplementationError();
               }
            } else {
               return null;
            }
         }

         public MaskElement visitMaskElementCmpBlock(MaskParser.MaskElementCmpBlockContext ctx) {
            MaskParser.CmpBlockContext cmpBlockContext = ctx.cmpBlock();
            MaskParser.MultiBlockMatchContext multiBlockMatchContext = ctx.multiBlockMatch();
            if (cmpBlockContext != null && multiBlockMatchContext != null) {
               MaskParser.NearContext nearContext = cmpBlockContext.near();
               String text = cmpBlockContext.getText();
               List<MaskElement> any = new ArrayList<>();

               for (MaskParser.BlockMatchContext blockMatch : multiBlockMatchContext.blockMatch()) {
                  BlockConditionWidget.BlockConditionState condition = CmdStringConverter.parseBlockCondition(
                     blockMatch.getText(), blockMatch.start.getStartIndex()
                  );
                  if (nearContext != null) {
                     MaskParser.NumericContext numeric = nearContext.numeric();
                     int radius = numeric == null ? 1 : (Integer)numeric.accept(new NumericVisitor());
                     any.add(new BlockNearConditionMaskElement(condition.createCondition(), List.of(condition), radius));
                  } else {
                     switch (text) {
                        case "block":
                           any.add(new BlockConditionMaskElement(condition.createCondition(), List.of(condition)));
                           break;
                        case "above":
                           any.add(new BlockAboveConditionMaskElement(condition.createCondition(), List.of(condition)));
                           break;
                        case "below":
                           any.add(new BlockBelowConditionMaskElement(condition.createCondition(), List.of(condition)));
                           break;
                        case "neighbor":
                           any.add(new BlockNeighborConditionMaskElement(condition.createCondition(), List.of(condition)));
                           break;
                        case "adjacent":
                           any.add(new BlockAdjacentConditionMaskElement(condition.createCondition(), List.of(condition)));
                           break;
                        default:
                           throw new FaultyImplementationError();
                     }
                  }
               }

               if (ctx.cmp.getType() == 38) {
                  return new AnyMaskElement(any.toArray(new MaskElement[0]));
               } else if (ctx.cmp.getType() == 43) {
                  return new NotMaskElement(new AnyMaskElement(any.toArray(new MaskElement[0])));
               } else {
                  throw new FaultyImplementationError();
               }
            } else {
               return null;
            }
         }

         public MaskElement visitMaskElementCmpNumeric(MaskParser.MaskElementCmpNumericContext ctx) {
            int value = (Integer)ctx.numeric().accept(new NumericVisitor());

            int comparison = switch (ctx.cmp.getType()) {
               case 38 -> 0;
               case 39 -> 2;
               case 40 -> 4;
               case 41 -> 3;
               case 42 -> 5;
               case 43 -> 1;
               default -> throw new FaultyImplementationError();
            };
            String var4 = ctx.cmpNumeric().getText();

            return (MaskElement)(switch (var4) {
               case "x" -> new CoordMaskElement(Axis.X, value, comparison);
               case "y" -> new CoordMaskElement(Axis.Y, value, comparison);
               case "z" -> new CoordMaskElement(Axis.Z, value, comparison);
               case "angle" -> new AngleMaskElement(value, comparison);
               default -> throw new FaultyImplementationError();
            });
         }

         public MaskElement visitMaskElementOffset(MaskParser.MaskElementOffsetContext ctx) {
            int offsetX = (Integer)ctx.numeric(0).accept(new NumericVisitor());
            int offsetY = (Integer)ctx.numeric(1).accept(new NumericVisitor());
            int offsetZ = (Integer)ctx.numeric(2).accept(new NumericVisitor());
            MaskElement inner = (MaskElement)ctx.maskElement().accept(this);
            return inner == null ? null : new OffsetMaskElement(inner, offsetX, offsetY, offsetZ);
         }
      };
      parser.setErrorHandler(
         new DefaultErrorStrategy() {
            protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
               ParserRuleContext context = recognizer.getRuleContext();
               if (context.getChildCount() == 0) {
                  String friendlyName = switch (context.getRuleIndex()) {
                     case 0, 1 -> "mask name";
                     case 2 -> "biome";
                     case 3 -> "block or block tag";
                     default -> null;
                     case 11 -> "number";
                  };
                  if (friendlyName != null) {
                     String msg = "expected " + friendlyName;
                     if (e.getOffendingToken().getType() != -1) {
                        msg = msg + ", got " + this.getTokenErrorDisplay(e.getOffendingToken());
                     }

                     recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
                     return;
                  }
               }

               IntervalSet expectedTokens = e.getExpectedTokens();
               String msg;
               if (e.getOffendingToken().getType() == -1) {
                  msg = "expected " + expectedTokens.toString(recognizer.getVocabulary()).replace("EQUALS", "'='");
               } else {
                  msg = "mismatched input "
                     + this.getTokenErrorDisplay(e.getOffendingToken())
                     + " expected "
                     + expectedTokens.toString(recognizer.getVocabulary()).replace("EQUALS", "'='");
               }

               recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
            }
         }
      );
      parser.removeErrorListeners();
      parser.addErrorListener(new ANTLRErrorListener() {
         public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            String symbolStr;
            int start;
            if (offendingSymbol instanceof Token token) {
               symbolStr = token.getText();
               start = token.getStartIndex();
            } else {
               symbolStr = offendingSymbol.toString();
               start = charPositionInLine;
            }

            throw new CmdStringConverter.CmdStringParseException(start, start + symbolStr.length() + 1, msg);
         }

         public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
         }

         public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
         }

         public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
         }
      });
      MaskElement maskElement = (MaskElement)visitor.visit(parser.mask());
      if (maskElement != null) {
         return maskElement;
      } else {
         throw new CmdStringConverter.CmdStringParseException(0, string.length(), "Unknown Error");
      }
   }

   private static Holder<Biome> parseBiome(String biomeStr, int start) {
      ResourceLocation resource;
      try {
         resource = ResourceLocation.parse(biomeStr);
      } catch (ResourceLocationException var5) {
         throw new CmdStringConverter.CmdStringParseException(start, start + biomeStr.length() + 1, "not a valid resource location");
      }

      Registry<Biome> registry = (Registry<Biome>)Minecraft.getInstance().getConnection().registryAccess().registry(Registries.BIOME).get();
      Optional<Reference<Biome>> biomeOpt = registry.getHolder(ResourceKey.create(Registries.BIOME, resource));
      if (biomeOpt.isEmpty()) {
         throw new CmdStringConverter.CmdStringParseException(start, start + biomeStr.length() + 1, "unknown biome");
      } else {
         return (Holder<Biome>)biomeOpt.get();
      }
   }

   private static BlockConditionWidget.BlockConditionState parseBlockCondition(String blockStr, int start) {
      if (blockStr.startsWith("#")) {
         blockStr = blockStr.substring("#".length());

         ResourceLocation resource;
         try {
            resource = ResourceLocation.parse(blockStr);
         } catch (ResourceLocationException var14) {
            throw new CmdStringConverter.CmdStringParseException(start, start + blockStr.length() + 1, "not a valid resource location");
         }

         TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, resource);
         Optional<Named<Block>> tagOpt = BuiltInRegistries.BLOCK.getTag(tagKey);
         if (tagOpt.isPresent()) {
            Named<Block> tag = tagOpt.get();
            BlockList.MinecraftOrCustomTagSet tagSet = new BlockList.MinecraftOrCustomTagSet(resource, tag, null);
            return new BlockConditionWidget.BlockConditionState(null, null, tagSet);
         } else {
            Set<ResourceLocation> localTag = ClientTagsHelper.getOrCreateLocalTag(tagKey);
            if (localTag != null && !localTag.isEmpty()) {
               BlockList.MinecraftOrCustomTagSet tagSet = new BlockList.MinecraftOrCustomTagSet(resource, null, localTag);
               return new BlockConditionWidget.BlockConditionState(null, null, tagSet);
            } else {
               if (!blockStr.contains(":")) {
                  tagOpt = BuiltInRegistries.BLOCK
                     .getTags()
                     .filter(pair -> ((TagKey)pair.getFirst()).location().getPath().equals(resource.getPath()))
                     .findAny()
                     .map(Pair::getSecond);
                  if (tagOpt.isPresent()) {
                     Named<Block> tag = tagOpt.get();
                     BlockList.MinecraftOrCustomTagSet tagSet = new BlockList.MinecraftOrCustomTagSet(resource, tag, null, null);
                     return new BlockConditionWidget.BlockConditionState(null, null, tagSet);
                  }

                  String[] searchLocations = new String[]{"axiom", "color", "material"};

                  for (String namespace : searchLocations) {
                     try {
                        ResourceLocation newResource = ResourceLocation.parse(namespace + ":" + blockStr);
                        tagKey = TagKey.create(Registries.BLOCK, newResource);
                        localTag = ClientTagsHelper.getOrCreateLocalTag(tagKey);
                        if (localTag != null && !localTag.isEmpty()) {
                           BlockList.MinecraftOrCustomTagSet tagSet = new BlockList.MinecraftOrCustomTagSet(newResource, null, localTag);
                           return new BlockConditionWidget.BlockConditionState(null, null, tagSet);
                        }
                     } catch (ResourceLocationException var13) {
                     }
                  }
               }

               throw new CmdStringConverter.CmdStringParseException(start, start + blockStr.length() + 1, "unknown block tag");
            }
         }
      } else {
         Set<String> nonDefaultProperties = new HashSet<>();
         CustomBlockState customBlockState = ServerCustomBlocks.deserialize(blockStr, nonDefaultProperties);
         if (customBlockState == null) {
            throw new CmdStringConverter.CmdStringParseException(start, start + blockStr.length() + 1, "unknown block");
         } else {
            Map<Property<?>, Comparable<?>> selectedProperties = new HashMap<>();

            for (Property<?> property : customBlockState.getProperties()) {
               if (nonDefaultProperties.contains(property.getName())) {
                  selectedProperties.put(property, customBlockState.getProperty((Property)property));
               } else {
                  selectedProperties.put(property, null);
               }
            }

            return new BlockConditionWidget.BlockConditionState(customBlockState.getCustomBlock(), selectedProperties, null);
         }
      }
   }

   public static String toCmdString(MaskElement maskElement) {
      StringBuilder stringBuilder = new StringBuilder();
      addToCmdString(stringBuilder, maskElement, new BooleanWrapper(false));
      String string = stringBuilder.toString();
      if (string.startsWith("(") && string.endsWith(")")) {
         string = string.substring(1, string.length() - 1);
      }

      return string;
   }

   private static void addToCmdString(StringBuilder stringBuilder, MaskElement maskElement, BooleanWrapper negate) {
      if (maskElement instanceof BothMaskElement bothMaskElement) {
         if (negate.value) {
            stringBuilder.append("!");
            negate.value = false;
         }

         stringBuilder.append("(");
         addToCmdString(stringBuilder, bothMaskElement.getChild1(), new BooleanWrapper(false));
         stringBuilder.append(" & ");
         addToCmdString(stringBuilder, bothMaskElement.getChild2(), new BooleanWrapper(false));
         stringBuilder.append(")");
      } else if (maskElement instanceof AllMaskElement allMaskElement) {
         if (negate.value) {
            stringBuilder.append("!");
            negate.value = false;
         }

         stringBuilder.append("(");
         MaskElement[] children = allMaskElement.getChildren();

         for (int i = 0; i < children.length; i++) {
            if (i > 0) {
               stringBuilder.append(" & ");
            }

            addToCmdString(stringBuilder, children[i], new BooleanWrapper(false));
         }

         stringBuilder.append(")");
      } else if (maskElement instanceof EitherMaskElement eitherMaskElement) {
         MaskElement left = eitherMaskElement.getChild1();
         MaskElement right = eitherMaskElement.getChild2();
         if (negate.value) {
            stringBuilder.append("!");
            negate.value = false;
         }

         stringBuilder.append("(");
         addToCmdString(stringBuilder, left, new BooleanWrapper(false));
         stringBuilder.append(" | ");
         addToCmdString(stringBuilder, right, new BooleanWrapper(false));
         stringBuilder.append(")");
      } else if (maskElement instanceof AnyMaskElement anyMaskElement) {
         MaskElement[] children = anyMaskElement.getChildren();
         if (negate.value) {
            stringBuilder.append("!");
            negate.value = false;
         }

         stringBuilder.append("(");

         for (int i = 0; i < children.length; i++) {
            MaskElement child = children[i];
            if (child != null) {
               if (i > 0) {
                  stringBuilder.append(" | ");
               }

               addToCmdString(stringBuilder, children[i], new BooleanWrapper(false));
            }
         }

         stringBuilder.append(")");
      } else if (maskElement instanceof NotMaskElement notMaskElement) {
         BooleanWrapper booleanWrapper = new BooleanWrapper(true);
         StringBuilder childBuilder = new StringBuilder();
         addToCmdString(childBuilder, notMaskElement.getChild(), booleanWrapper);
         if (booleanWrapper.value) {
            stringBuilder.append("!(");
            stringBuilder.append((CharSequence)childBuilder);
            stringBuilder.append(")");
         } else {
            stringBuilder.append((CharSequence)childBuilder);
         }
      } else if (maskElement instanceof OffsetMaskElement offsetMaskElement) {
         stringBuilder.append("offset(");
         stringBuilder.append(offsetMaskElement.getOffsetX());
         stringBuilder.append(",");
         stringBuilder.append(offsetMaskElement.getOffsetY());
         stringBuilder.append(",");
         stringBuilder.append(offsetMaskElement.getOffsetZ());
         stringBuilder.append("){ ");
         addToCmdString(stringBuilder, offsetMaskElement.getChild(), new BooleanWrapper(false));
         stringBuilder.append(" }");
      } else if (maskElement instanceof BiomeConditionMaskElement biomeConditionMaskElement) {
         if (negate.value) {
            stringBuilder.append("biome != ");
            negate.value = false;
         } else {
            stringBuilder.append("biome = ");
         }

         stringBuilder.append(convertResource(biomeConditionMaskElement.getMatchBiome().location()));
      } else if (maskElement instanceof GenericBlockConditionMaskElement blockConditionMaskElement) {
         stringBuilder.append(blockConditionMaskElement.cmdStringName());
         if (negate.value) {
            stringBuilder.append(" != ");
            negate.value = false;
         } else {
            stringBuilder.append(" = ");
         }

         appendBlockConditions(stringBuilder, blockConditionMaskElement.getConditionStates());
      } else if (maskElement instanceof CoordMaskElement coordMaskElement) {
         stringBuilder.append(coordMaskElement.getAxis().getName().toLowerCase(Locale.ROOT));
         stringBuilder.append(" ");

         stringBuilder.append(switch (coordMaskElement.getComparison()) {
            case 0 -> "=";
            case 1 -> "!=";
            case 2 -> "<";
            case 3 -> ">";
            case 4 -> "<=";
            case 5 -> ">=";
            default -> throw new FaultyImplementationError();
         });
         stringBuilder.append(" ").append(coordMaskElement.getValue());
      } else if (maskElement instanceof AngleMaskElement angleMaskElement) {
         stringBuilder.append("angle ");

         stringBuilder.append(switch (angleMaskElement.getComparison()) {
            case 0 -> "=";
            case 1 -> "!=";
            case 2 -> "<";
            case 3 -> ">";
            case 4 -> "<=";
            case 5 -> ">=";
            default -> throw new FaultyImplementationError();
         });
         stringBuilder.append(" ").append(angleMaskElement.getAngle());
      } else {
         if (!(maskElement instanceof GenericSingleMaskElement genericSingleMaskElement)) {
            throw new UnsupportedOperationException("Don't know how to convert " + maskElement.getClass());
         }

         if (negate.value) {
            stringBuilder.append("!");
            negate.value = false;
         }

         stringBuilder.append(genericSingleMaskElement.cmdStringName());
      }
   }

   private static void appendBlockConditions(StringBuilder stringBuilder, List<BlockConditionWidget.BlockConditionState> conditions) {
      if (conditions.size() == 1) {
         appendBlockCondition(stringBuilder, conditions.get(0));
      } else {
         boolean first = true;
         stringBuilder.append("[");

         for (BlockConditionWidget.BlockConditionState condition : conditions) {
            if (first) {
               first = false;
            } else {
               stringBuilder.append(",");
            }

            appendBlockCondition(stringBuilder, condition);
         }

         stringBuilder.append("]");
      }
   }

   private static void appendBlockCondition(StringBuilder stringBuilder, BlockConditionWidget.BlockConditionState condition) {
      if (condition.tag() != null) {
         stringBuilder.append("#");
         stringBuilder.append(convertResource(condition.tag().name()));
      } else {
         stringBuilder.append(convertResource(condition.block().axiom$getIdentifier()));
         Map<Property<?>, Comparable<?>> map = condition.selectedProperties();
         boolean empty = true;

         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            if (entry.getValue() != null) {
               if (empty) {
                  stringBuilder.append("[");
                  empty = false;
               } else {
                  stringBuilder.append(",");
               }

               stringBuilder.append(entry.getKey().getName());
               stringBuilder.append("=");
               stringBuilder.append(entry.getValue().toString().toLowerCase(Locale.ROOT));
            }
         }

         if (!empty) {
            stringBuilder.append("]");
         }
      }
   }

   private static String convertResource(ResourceLocation resourceLocation) {
      return resourceLocation.getNamespace().equals("minecraft") ? resourceLocation.getPath() : resourceLocation.toString();
   }

   public static final class CmdStringParseException extends RuntimeException {
      public final int start;
      public final int end;
      public final String message;

      public CmdStringParseException(int start, int end, String message) {
         this.start = start;
         this.end = end;
         this.message = message;
      }
   }
}
