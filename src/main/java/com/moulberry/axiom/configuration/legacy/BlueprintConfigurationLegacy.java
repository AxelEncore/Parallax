package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.configuration.AxiomConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;

class BlueprintConfigurationLegacy extends LegacyAbstractConfigurationCategory {
   public boolean automaticRefreshing = this.load(Boolean.class, "automaticRefreshing", true);
   public boolean recursiveSearch = this.load(Boolean.class, "recursiveSearch", false);
   public String customPath = this.load(String.class, "customPath", "");
   public String defaultTags = this.load(
      String.class,
      "defaultTags",
      "small,medium,large,massive,organic,structure,terrain,interior,house,tower,bridge,castle,statue,temple,monument,barn,stable,windmill,store,watermill,ship,airship,balloon,palace,watchtower,mansion,grave,marketplace,mine,obelisk,warehouse,silo,shipwreck,mausoleum,cemetery,bunker,airplane,helicopter,car,truck,vehicle,blacksmith,crypt,factory,mountain,cliff,rock,iceberg,spike,stone,wood,brick,natural,sand,metal,winter,spring,summer,autumn,tree,bush,mushroom,spruce,oak,birch,coniferous,deciduous,acacia,mangrove,cherryblossom,darkoak,jungle,baobab,azalea,cypress,coral,sapling,grass,seagrass,bamboo,flowers,animal,creature,dead,lamp,streetlight,brazier,bed,bookshelf,closet,table,chair,fireplace,carpet,fountain,clock,banner,flag,bell,modern,medieval,steampunk,gothic,oriental,victorian,fantasy,sci-fi,elven,dwarven,futuristic,retro,classic,rustic,baroque,rococo,industrial,artnouveau,artdeco,cyberpunk,space,arabic,indian,egyptian,greek,roman,norse,mesoamerican,japanese,western,spanish,tudor,spooky,pirate,dungeons,rubble,crates,redstone,wall,window,roof,stairs,pillar,arch,stairs,chimney,well"
   );

   public BlueprintConfigurationLegacy(CommentedConfigurationNode node) {
      super(node);
   }

   @Override
   public void applyToNewConfiguration(AxiomConfig newConfiguration) {
      newConfiguration.blueprint.automaticRefreshing = this.automaticRefreshing;
      newConfiguration.blueprint.recursiveSearch = this.recursiveSearch;
      newConfiguration.blueprint.customPath = this.customPath;
      newConfiguration.blueprint.defaultTags = this.defaultTags;
   }
}
