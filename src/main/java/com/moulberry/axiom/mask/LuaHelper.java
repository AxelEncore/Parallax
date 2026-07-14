package com.moulberry.axiom.mask;

import com.mojang.datafixers.util.Pair;
import com.moulberry.axiom.DefaultBlocks;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2dToIntMap;
import com.moulberry.axiom.noise.SimplexNoise;
import com.moulberry.axiom.noise.VoronoiEdgesNoise;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import com.moulberry.axiom.utils.OkLabColourUtils;
import com.moulberry.axiom.utils.Utf8ChatOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaNil;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

public class LuaHelper {
   private static final LuaValue LUA_X = LuaValue.valueOf("x");
   private static final LuaValue LUA_Y = LuaValue.valueOf("y");
   private static final LuaValue LUA_Z = LuaValue.valueOf("z");
   private static final LuaValue LUA_PLAYER_X = LuaValue.valueOf("player_x");
   private static final LuaValue LUA_PLAYER_Y = LuaValue.valueOf("player_y");
   private static final LuaValue LUA_PLAYER_Z = LuaValue.valueOf("player_z");
   private static final LuaValue LUA_PLAYER_YAW = LuaValue.valueOf("player_yaw");
   private static final LuaValue LUA_PLAYER_PITCH = LuaValue.valueOf("player_pitch");
   private static final LuaValue LUA_ACTIVE_BLOCK_STATE = LuaValue.valueOf("activeBlockState");
   private static LuaTable luaTableBlock = null;

   public static String getAvailableLuaFunctions(boolean allowSetBlock) {
      String functions = "Custom variables:\nx, y, z -> coords of target location\nblocks.abc -> retrieve the id for a Block, eg. blocks.stone for the id of stone\nplayer_x, player_y, player_z, player_yaw, player_pitch -> position of player when executing script\nactiveBlockState -> the BlockState in the 'Active Block State' window\n\nCustom functions:\ngetBlock(x, y, z) -> get the Block id at position xyz\ngetBlockState(x, y, z) -> get the BlockState id at position xyz\ngetHighestBlockYAt(x, z) -> get the y coordinate of the highest solid block at xz\ngetSimplexNoise(x, y, z, (seed)) -> sample simplex noise at position x, y, z\ngetVoronoiEdgeNoise(x, y, z, (seed)) -> sample voronoi edge noise at position xyz\nisSolid(block) -> get if the block argument is solid\nisBlockTagged(block, \"tag\") -> get if the block argument has the given block tag\nwithBlockProperty(block, \"property=value\", ...) -> update property to value for block\ngetBlockProperty(block, \"property\") -> gets the value of a property as a string, or nil if it doesn't exist\ngetBlockRGB(block) -> gets the average rgb of the given block\nfindClosestBlockToRGB(rgb, (flags), (index)) -> finds the block that's closest to the given RGB value\nisSelected(x, y, z) -> get if position xyz is selected, or true if there is no selection\n";
      if (allowSetBlock) {
         functions = functions + "setBlock(x, y, z, block) -> set an additional block at the position x, y, z\n";
      }

      return functions;
   }

   public static Globals createSandboxed() {
      Globals globals = new Globals();
      globals.load(new JseBaseLib());
      globals.load(new Bit32Lib());
      globals.load(new TableLib());
      globals.load(new JseStringLib());
      globals.load(new JseMathLib());
      globals.set("dofile", LuaValue.NIL);
      globals.set("loadfile", LuaValue.NIL);
      globals.set("collectgarbage", LuaValue.NIL);
      LuaNil.s_metatable = makeReadOnly(LuaNil.s_metatable);
      LuaNumber.s_metatable = makeReadOnly(LuaNumber.s_metatable);
      LuaBoolean.s_metatable = makeReadOnly(LuaBoolean.s_metatable);
      LuaString.s_metatable = makeReadOnly(LuaString.s_metatable);
      LuaFunction.s_metatable = makeReadOnly(LuaFunction.s_metatable);
      LuaThread.s_metatable = makeReadOnly(LuaThread.s_metatable);
      LoadState.install(globals);
      LuaC.install(globals);
      globals.finder = null;
      globals.STDOUT = new PrintStream(new Utf8ChatOutputStream(ChatFormatting.WHITE), false, StandardCharsets.UTF_8);
      globals.STDERR = new PrintStream(new Utf8ChatOutputStream(ChatFormatting.RED), false, StandardCharsets.UTF_8);
      return globals;
   }

   public static LuaFunction compile(String script, Globals globals) {
      try {
         Prototype prototype = LuaC.instance.compile(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), "script");
         LuaJavaLoader loader = new LuaJavaLoader(LuaHelper.class.getClassLoader());
         String classname = "com.moulberry.axiom.dynamic.LuaScript" + ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
         return loader.load(prototype, classname, "script.lua", globals);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }
   }

   public static int blockToInternalId(Block block) {
      return BuiltInRegistries.BLOCK.getId(block);
   }

   public static int stateToInternalId(BlockState blockState) {
      return -Block.BLOCK_STATE_REGISTRY.getId(blockState) - 1;
   }

   @Nullable
   public static BlockState internalIdToState(int id) {
      if (id < 0) {
         int stateId = -id - 1;
         return (BlockState)Block.BLOCK_STATE_REGISTRY.byId(stateId);
      } else {
         Optional<Reference<Block>> holderOpt = BuiltInRegistries.BLOCK.getHolder(id);
         return !holderOpt.isPresent() ? null : DefaultBlocks.forBlock((Block)holderOpt.get().value());
      }
   }

   @Nullable
   private static Named<Block> lookupTag(String tag, Map<String, Optional<Named<Block>>> cache) {
      Optional<Named<Block>> cached = cache.get(tag);
      if (cached != null) {
         return cached.orElse(null);
      } else {
         ResourceLocation resourceLocation = ResourceLocation.parse(tag);
         Optional<Named<Block>> tagOpt = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, resourceLocation));
         if (tagOpt.isEmpty()) {
            tagOpt = BuiltInRegistries.BLOCK
               .getTags()
               .filter(pair -> ((TagKey)pair.getFirst()).location().getPath().equals(tag))
               .findAny()
               .map(Pair::getSecond);
         }

         cache.put(tag, tagOpt);
         return tagOpt.orElse(null);
      }
   }

   public static void initializeGeneric(Globals globals, @Nullable LuaHelper.SetBlockInterface setBlockInterface) {
      final ClientLevel level = Minecraft.getInstance().level;
      final MutableBlockPos mutableBlockPos = new MutableBlockPos();
      final Position2dToIntMap heightmap = new Position2dToIntMap(Integer.MIN_VALUE);
      globals.set(LUA_X, LuaValue.valueOf(0));
      globals.set(LUA_Y, LuaValue.valueOf(0));
      globals.set(LUA_Z, LuaValue.valueOf(0));
      updateExtraVariables(globals);
      globals.set("getBlock", new ThreeArgFunction() {
         public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            BlockState blockState = level.getBlockState(mutableBlockPos.set(x.toint(), y.toint(), z.toint()));
            return LuaValue.valueOf(LuaHelper.blockToInternalId(blockState.getBlock()));
         }
      });
      globals.set("getBlockState", new ThreeArgFunction() {
         public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            BlockState blockState = level.getBlockState(mutableBlockPos.set(x.toint(), y.toint(), z.toint()));
            return LuaValue.valueOf(LuaHelper.stateToInternalId(blockState));
         }
      });
      globals.set("isSolid", new OneArgFunction() {
         public LuaValue call(LuaValue arg) {
            int id = arg.toint();
            BlockState blockState = LuaHelper.internalIdToState(id);
            return (LuaValue)(blockState == null ? LuaValue.NIL : LuaValue.valueOf(blockState.blocksMotion()));
         }
      });
      final Map<String, Optional<Named<Block>>> cachedBlockTags = new HashMap<>();
      globals.set("isBlockTagged", new TwoArgFunction() {
         public LuaValue call(LuaValue arg1, LuaValue arg2) {
            int id = arg1.toint();
            BlockState blockState = LuaHelper.internalIdToState(id);
            if (blockState == null) {
               return LuaValue.NIL;
            } else {
               String tag = arg2.tojstring().toLowerCase(Locale.ROOT);
               Named<Block> set = LuaHelper.lookupTag(tag, cachedBlockTags);
               return (LuaValue)(set == null ? LuaValue.error("tag '" + tag + "' doesn't exist") : LuaValue.valueOf(blockState.is(set)));
            }
         }
      });
      globals.set(
         "getFluidBlockStateOrAir",
         new OneArgFunction() {
            public LuaValue call(LuaValue arg) {
               int id = arg.toint();
               BlockState blockState = LuaHelper.internalIdToState(id);
               return (LuaValue)(blockState == null
                  ? LuaValue.NIL
                  : LuaValue.valueOf(LuaHelper.stateToInternalId(blockState.getFluidState().createLegacyBlock())));
            }
         }
      );
      final int randomSeed = ThreadLocalRandom.current().nextInt();
      globals.set(
         "getSimplexNoise",
         new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
               LuaValue seedArg = args.arg(4);
               double x = args.arg(1).todouble();
               double y = args.arg(2).todouble();
               double z = args.arg(3).todouble();
               return !seedArg.isnil()
                  ? LuaValue.valueOf(SimplexNoise.evaluateStatic(x, y, z, seedArg.toint()))
                  : LuaValue.valueOf(SimplexNoise.evaluateStatic(x, y, z, randomSeed));
            }
         }
      );
      globals.set(
         "getVoronoiEdgeNoise",
         new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
               LuaValue seedArg = args.arg(4);
               double x = args.arg(1).todouble();
               double y = args.arg(2).todouble();
               double z = args.arg(3).todouble();
               return !seedArg.isnil()
                  ? LuaValue.valueOf(VoronoiEdgesNoise.evaluateStatic(x, y, z, seedArg.toint(), 1.0F))
                  : LuaValue.valueOf(VoronoiEdgesNoise.evaluateStatic(x, y, z, randomSeed, 1.0F));
            }
         }
      );
      globals.set("getBlockProperty", new TwoArgFunction() {
         public LuaValue call(LuaValue arg1, LuaValue arg2) {
            int block = arg1.toint();
            BlockState blockState = LuaHelper.internalIdToState(block);
            if (blockState == null) {
               return LuaValue.NIL;
            } else {
               StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();
               Property<?> property = stateDefinition.getProperty(arg2.tojstring().trim().toLowerCase(Locale.ROOT));
               return (LuaValue)(property == null ? LuaValue.NIL : LuaValue.valueOf(LuaHelper.serialize(blockState, property)));
            }
         }
      });
      globals.set("getBlockRGB", new OneArgFunction() {
         public LuaValue call(LuaValue arg1) {
            int block = arg1.toint();
            BlockState blockState = LuaHelper.internalIdToState(block);
            if (blockState == null) {
               return LuaValue.NIL;
            } else {
               Vec3 lab = BlockColourMap.getLab(blockState.getBlock());
               return (LuaValue)(lab == null ? LuaValue.NIL : LuaValue.valueOf(OkLabColourUtils.lab2rgb(lab.x, lab.y, lab.z)));
            }
         }
      });
      globals.set("findClosestBlockToRGB", new VarArgFunction() {
         public LuaValue onInvoke(Varargs args) {
            int rgb = args.arg(1).toint();
            int flags = BlockColourMap.FLAG_SOLID | BlockColourMap.FLAG_OPAQUE | BlockColourMap.FLAG_FULL_CUBE;
            if (!args.arg(2).isnil()) {
               flags = args.arg(2).toint();
            }

            int index = 1;
            if (!args.arg(3).isnil()) {
               index = args.arg(3).toint();
            }

            if (index <= 0) {
               return LuaValue.error("index must be greater than zero");
            } else {
               double[] lab = new double[3];
               OkLabColourUtils.rgb2lab(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, lab);
               List<BlockState> list = BlockColourMap.getNearestLabN(lab[0], lab[1], lab[2], flags, index);
               BlockState blockState = list.get(list.size() - 1);
               return LuaValue.valueOf(LuaHelper.stateToInternalId(blockState));
            }
         }
      });
      globals.set("withBlockProperty", new VarArgFunction() {
         public Varargs onInvoke(Varargs args) {
            int block = args.toint(1);
            BlockState blockState = LuaHelper.internalIdToState(block);
            StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();
            int count = args.narg() - 1;

            for (int i = 0; i < count; i++) {
               String string = args.tojstring(i + 2);

               for (String propertySetter : string.split(",")) {
                  String[] split = propertySetter.split("=");
                  if (split.length < 2) {
                     return LuaValue.argerror(i + 2, "missing equals sign. for example 'facing=west'");
                  }

                  String propertyName = split[0].trim().toLowerCase(Locale.ROOT);
                  Property<?> property = stateDefinition.getProperty(propertyName);
                  if (property != null) {
                     String propertyValue = split[1].trim().toLowerCase(Locale.ROOT);
                     blockState = ItemStackDataHelper.updateStateString(blockState, property, propertyValue);
                  }
               }
            }

            return LuaValue.valueOf(LuaHelper.stateToInternalId(blockState));
         }
      });
      globals.set("getHighestBlockYAt", new TwoArgFunction() {
         public LuaValue call(LuaValue x, LuaValue z) {
            int xi = x.toint();
            int zi = z.toint();
            int currentValue = heightmap.get(xi, zi);
            if (currentValue != Integer.MIN_VALUE) {
               return LuaValue.valueOf(currentValue);
            } else {
               int y = level.getMaxBuildHeight() - 1;

               while (true) {
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(xi, y, zi));
                  if (blockState.getBlock() == Blocks.VOID_AIR || blockState.blocksMotion()) {
                     heightmap.put(xi, zi, y);
                     return LuaValue.valueOf(y);
                  }

                  y--;
               }
            }
         }
      });
      if (setBlockInterface != null) {
         globals.set("setBlock", new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
               int x = args.arg(1).toint();
               int y = args.arg(2).toint();
               int z = args.arg(3).toint();
               int id = args.arg(4).toint();
               BlockState blockState = LuaHelper.internalIdToState(id);
               if (blockState != null) {
                  setBlockInterface.setBlock(x, y, z, blockState);
               }

               return LuaValue.NIL;
            }
         });
      }

      globals.set("isSelected", new ThreeArgFunction() {
         public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            return LuaValue.valueOf(Selection.contains(x.toint(), y.toint(), z.toint()));
         }
      });
      globals.set("blocks", getBlockTable());
   }

   private static <T extends Comparable<T>> String serialize(BlockState blockState, Property<T> property) {
      T comparable = (T)blockState.getValue(property);
      return property.getName(comparable);
   }

   public static void initializeMask(Globals globals, MaskContext maskContext, int x, int y, int z) {
      globals.set(LUA_X, LuaValue.valueOf(x));
      globals.set(LUA_Y, LuaValue.valueOf(y));
      globals.set(LUA_Z, LuaValue.valueOf(z));
      updateExtraVariables(globals);
      globals.set("getBlock", new ThreeArgFunction() {
         public LuaValue call(LuaValue x, LuaValue yx, LuaValue zx) {
            BlockState blockState = maskContext.getBlockStateAt(x.toint(), yx.toint(), zx.toint());
            return LuaValue.valueOf(LuaHelper.blockToInternalId(blockState.getBlock()));
         }
      });
      globals.set("getBlockState", new ThreeArgFunction() {
         public LuaValue call(LuaValue x, LuaValue yx, LuaValue zx) {
            BlockState blockState = maskContext.getBlockStateAt(x.toint(), yx.toint(), zx.toint());
            return LuaValue.valueOf(LuaHelper.stateToInternalId(blockState));
         }
      });
      globals.set("isSolid", new OneArgFunction() {
         public LuaValue call(LuaValue arg) {
            int id = arg.toint();
            BlockState blockState = LuaHelper.internalIdToState(id);
            return (LuaValue)(blockState == null ? LuaValue.NIL : LuaValue.valueOf(blockState.blocksMotion()));
         }
      });
      final Map<String, Optional<Named<Block>>> cachedBlockTags = new HashMap<>();
      globals.set("isBlockTagged", new TwoArgFunction() {
         public LuaValue call(LuaValue arg1, LuaValue arg2) {
            int id = arg1.toint();
            BlockState blockState = LuaHelper.internalIdToState(id);
            if (blockState == null) {
               return LuaValue.NIL;
            } else {
               String tag = arg2.tojstring().toLowerCase(Locale.ROOT);
               Named<Block> set = LuaHelper.lookupTag(tag, cachedBlockTags);
               return (LuaValue)(set == null ? LuaValue.error("tag '" + tag + "' doesn't exist") : LuaValue.valueOf(blockState.is(set)));
            }
         }
      });
      globals.set(
         "getFluidBlockStateOrAir",
         new OneArgFunction() {
            public LuaValue call(LuaValue arg) {
               int id = arg.toint();
               BlockState blockState = LuaHelper.internalIdToState(id);
               return (LuaValue)(blockState == null
                  ? LuaValue.NIL
                  : LuaValue.valueOf(LuaHelper.stateToInternalId(blockState.getFluidState().createLegacyBlock())));
            }
         }
      );
      final int randomSeed = ThreadLocalRandom.current().nextInt();
      globals.set(
         "getSimplexNoise",
         new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
               LuaValue seedArg = args.arg(4);
               double xx = args.arg(1).todouble();
               double yx = args.arg(2).todouble();
               double zx = args.arg(3).todouble();
               return !seedArg.isnil()
                  ? LuaValue.valueOf(SimplexNoise.evaluateStatic(xx, yx, zx, seedArg.toint()))
                  : LuaValue.valueOf(SimplexNoise.evaluateStatic(xx, yx, zx, randomSeed));
            }
         }
      );
      globals.set(
         "getVoronoiEdgeNoise",
         new VarArgFunction() {
            public Varargs onInvoke(Varargs args) {
               LuaValue seedArg = args.arg(4);
               double xx = args.arg(1).todouble();
               double yx = args.arg(2).todouble();
               double zx = args.arg(3).todouble();
               return !seedArg.isnil()
                  ? LuaValue.valueOf(VoronoiEdgesNoise.evaluateStatic(xx, yx, zx, seedArg.toint(), 1.0F))
                  : LuaValue.valueOf(VoronoiEdgesNoise.evaluateStatic(xx, yx, zx, randomSeed, 1.0F));
            }
         }
      );
      globals.set("getBlockProperty", new TwoArgFunction() {
         public LuaValue call(LuaValue arg1, LuaValue arg2) {
            int block = arg1.toint();
            BlockState blockState = LuaHelper.internalIdToState(block);
            StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();
            Property<?> property = stateDefinition.getProperty(arg2.tojstring().trim().toLowerCase(Locale.ROOT));
            return (LuaValue)(property == null ? LuaValue.NIL : LuaValue.valueOf(LuaHelper.serialize(blockState, property)));
         }
      });
      globals.set("withBlockProperty", new ThreeArgFunction() {
         public LuaValue call(LuaValue block, LuaValue propertyName, LuaValue propertyValue) {
            BlockState blockState = LuaHelper.internalIdToState(block.toint());
            StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();
            Property<?> property = stateDefinition.getProperty(propertyName.tojstring());
            if (property != null) {
               blockState = ItemStackDataHelper.updateStateString(blockState, property, propertyValue.tojstring());
            }

            return LuaValue.valueOf(LuaHelper.stateToInternalId(blockState));
         }
      });
      globals.set("getHighestBlockYAt", new TwoArgFunction() {
         public LuaValue call(LuaValue x, LuaValue zx) {
            return LuaValue.valueOf(maskContext.getHighestBlock(x.toint(), zx.toint()));
         }
      });
      globals.set("isSelected", new ThreeArgFunction() {
         public LuaValue call(LuaValue x, LuaValue yx, LuaValue zx) {
            return LuaValue.valueOf(Selection.contains(x.toint(), yx.toint(), zx.toint()));
         }
      });
      globals.set("blocks", getBlockTable());
   }

   public static void setPosition(Globals globals, int x, int y, int z) {
      globals.set(LUA_X, LuaValue.valueOf(x));
      globals.set(LUA_Y, LuaValue.valueOf(y));
      globals.set(LUA_Z, LuaValue.valueOf(z));
   }

   public static void updateExtraVariables(Globals globals) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         globals.set(LUA_PLAYER_X, LuaValue.valueOf(player.getX()));
         globals.set(LUA_PLAYER_Y, LuaValue.valueOf(player.getY()));
         globals.set(LUA_PLAYER_Z, LuaValue.valueOf(player.getZ()));
         globals.set(LUA_PLAYER_YAW, LuaValue.valueOf(player.getYRot()));
         globals.set(LUA_PLAYER_PITCH, LuaValue.valueOf(player.getXRot()));
      }

      globals.set(LUA_ACTIVE_BLOCK_STATE, LuaValue.valueOf(stateToInternalId(Tool.getActiveBlock())));
   }

   private static LuaTable getBlockTable() {
      if (luaTableBlock != null) {
         return luaTableBlock;
      } else {
         luaTableBlock = new LuaTable();
         Map<String, LuaTable> subTables = new HashMap<>();

         for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            int value = BuiltInRegistries.BLOCK.getId(block);
            String namespace = key.getNamespace();
            if (namespace.equals("minecraft")) {
               luaTableBlock.set(key.getPath(), value);
            } else {
               if (!subTables.containsKey(namespace)) {
                  LuaTable subTable = new LuaTable();
                  luaTableBlock.set(namespace, subTable);
                  subTables.put(namespace, subTable);
               }

               subTables.get(namespace).set(key.getPath(), value);
            }
         }

         return luaTableBlock;
      }
   }

   public static LuaTable makeReadOnly(LuaValue value) {
      if (value == null) {
         return null;
      } else {
         return value instanceof LuaHelper.ReadOnlyLuaTable readOnlyLuaTable ? readOnlyLuaTable : new LuaHelper.ReadOnlyLuaTable(value);
      }
   }

   static class ReadOnlyLuaTable extends LuaTable {
      public ReadOnlyLuaTable(LuaValue table) {
         this.presize(table.length(), 0);

         for (Varargs n = table.next(LuaValue.NIL); !n.arg1().isnil(); n = table.next(n.arg1())) {
            LuaValue key = n.arg1();
            LuaValue value = n.arg(2);
            super.rawset(key, (LuaValue)(value.istable() ? new LuaHelper.ReadOnlyLuaTable(value) : value));
         }
      }

      public LuaValue setmetatable(LuaValue metatable) {
         return error("table is read-only");
      }

      public void set(int key, LuaValue value) {
         error("table is read-only");
      }

      public void rawset(int key, LuaValue value) {
         error("table is read-only");
      }

      public void rawset(LuaValue key, LuaValue value) {
         error("table is read-only");
      }

      public LuaValue remove(int pos) {
         return error("table is read-only");
      }
   }

   @FunctionalInterface
   public interface SetBlockInterface {
      void setBlock(int var1, int var2, int var3, BlockState var4);
   }
}
