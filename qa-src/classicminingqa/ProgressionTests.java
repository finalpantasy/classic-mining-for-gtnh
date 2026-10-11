package classicminingqa;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.*;
import java.io.*;import java.lang.reflect.*;import java.security.*;import java.util.*;
import static classicmining.Reflection.*;

@GameTestHolder("classicminingqa")
public class ProgressionTests {
 static final Map<String,Integer> placeholders=new TreeMap<>();
 static boolean semantic=false;
 static String item(Object stack)throws Exception {
  Object it=call(method(stack.getClass(),0,"getItem","func_77973_b"),stack);
  Object id;
  if(it==null)id="null-item";
  else {
   Field registryField;try{registryField=type("net.minecraft.item.Item").getField("itemRegistry");}catch(NoSuchFieldException ex){registryField=type("net.minecraft.item.Item").getField("field_150901_e");}
   Object registry=registryField.get(null);
   id=call(method(registry.getClass(),1,"getNameForObject","func_148750_c"),registry,it);
   if(id==null)id="unregistered:"+it.getClass().getName()+":"+call(method(it.getClass(),0,"getUnlocalizedName","func_77658_a"),it);
  }
  if(!semantic&&(id.toString().startsWith("unregistered:")||id.toString().equals("null-item")))placeholders.put(id.toString(),placeholders.getOrDefault(id.toString(),0)+1);
  Object damage=call(method(stack.getClass(),0,"getItemDamage","func_77960_j"),stack);
  if(semantic&&it!=null) {
   Object info=call(method(type("gregtech.common.ores.OreManager"),1,"getOreInfo"),null,stack);
   if(info!=null) {
    Object material=field(info,"material"),stone=field(info,"stoneType");
    if(!(stone instanceof Enum))throw new IllegalStateException("Stable stone identity required");
    id="nativeOre:"+material.getClass().getName()+":"+classicmining.MiningHooks.bridge().material(material)+":"+((Enum<?>)stone).name()+":"+field(info,"isSmall")+":"+field(info,"isNatural");damage=0;
   }
  }
  return id+":"+damage+"x"+NativeMiningTests.size(stack)+":"+NativeMiningTests.get(stack,"stackTagCompound","field_77990_d");
 }
 static String canonical(Object o)throws Exception {
  if(o==null)return "null";
  if(type("net.minecraft.item.ItemStack").isInstance(o))return item(o);
  if(o.getClass().isArray()){StringBuilder s=new StringBuilder("[");for(int i=0;i<Array.getLength(o);i++)s.append(canonical(Array.get(o,i))).append(';');return s.append(']').toString();}
  if(o instanceof Collection){List<String> s=new ArrayList<>();for(Object v:(Collection<?>)o)s.add(canonical(v));Collections.sort(s);return s.toString();}
  if(o instanceof Number || o instanceof Boolean || o instanceof String)return o.toString();
  if(type("net.minecraftforge.fluids.FluidStack").isInstance(o)) {
   Object f=call(method(o.getClass(),0,"getFluid"),o);
   return call(method(f.getClass(),0,"getName"),f)+"x"+field(o,"amount")+":"+field(o,"tag");
  }
  throw new IllegalStateException("Unsupported parity value "+o.getClass());
 }
 static String hash(List<String> rows)throws Exception {
  Collections.sort(rows);MessageDigest md=MessageDigest.getInstance("SHA-256");
  for(String r:rows)md.update((r+"\n").getBytes("UTF-8"));StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format("%02x",b&255));return s.toString();
 }
 static String inputs(Object r,String values,String alternatives,String chanceField)throws Exception {
  Object a=field(r,values),alt=null,chances=chanceField==null?null:field(r,chanceField);
  try{alt=field(r,alternatives);}catch(IllegalStateException ignored){}
  StringBuilder s=new StringBuilder("[");
  for(int i=0;i<Array.getLength(a);i++) {
   Object choices=alt!=null&&i<Array.getLength(alt)?Array.get(alt,i):null;
   List<String> set=new ArrayList<>();
   if(choices!=null&&Array.getLength(choices)>0)for(int j=0;j<Array.getLength(choices);j++)set.add(canonical(Array.get(choices,j)));
   else set.add(canonical(Array.get(a,i)));
   Collections.sort(set);s.append(set).append('@').append(chances!=null&&i<Array.getLength(chances)?Array.get(chances,i):10000).append(';');
  }
  return s.append(']').toString();
 }
 static String outputs(Object r,String values,String chanceField)throws Exception {
  Object a=field(r,values),chances=field(r,chanceField);List<String> entries=new ArrayList<>();
  for(int i=0;i<Array.getLength(a);i++)entries.add(canonical(Array.get(a,i))+"@"+(chances!=null&&i<Array.getLength(chances)?Array.get(chances,i):10000));
  Collections.sort(entries);return entries.toString();
 }
 static String semanticRecipe(Object r)throws Exception {
  semantic=true;try {
   StringBuilder s=new StringBuilder();
   s.append("inputs=").append(inputs(r,"mInputs","mOreDictAlt","mInputChances"));
   s.append("|fluidInputs=").append(inputs(r,"mFluidInputs","mAltFluidInputs","mFluidInputChances"));
   s.append("|outputs=").append(outputs(r,"mOutputs","mOutputChances"));
   s.append("|fluidOutputs=").append(outputs(r,"mFluidOutputs","mFluidOutputChances"));
   for(String f:new String[]{"mDuration","mEUt","mSpecialValue","mEnabled","mFakeRecipe","mHidden","mCanBeBuffered","mNeedsEmptyOutput","isNBTSensitive"})s.append('|').append(f).append('=').append(canonical(field(r,f)));
   return s.toString();
  } finally{semantic=false;}
 }
 @GameTest(timeoutTicks=200,batch="classicmining_progression")
 public static void automationAndRecipes(GameTestHelper helper)throws Exception {
  int recipes=0,voidDims=0,sg=0;
  try(PrintWriter p=new PrintWriter(new File(MiningWorldTests.output(),"progression-parity.csv"))) {
   p.println("kind,name,entries,sha256");
   Map<?,?> maps=(Map<?,?>)type("gregtech.api.recipe.RecipeMap").getField("ALL_RECIPE_MAPS").get(null);
   for(Map.Entry<?,?> e:maps.entrySet()) {
    List<String> rows=new ArrayList<>(),normalized=new ArrayList<>();
    for(Object r:(Collection<?>)call(method(e.getValue().getClass(),0,"getAllRecipes"),e.getValue())) {
     StringBuilder s=new StringBuilder();
     for(String f:new String[]{"mInputs","mOutputs","mFluidInputs","mFluidOutputs","mInputChances","mOutputChances","mFluidInputChances","mFluidOutputChances","mDuration","mEUt","mSpecialValue","mEnabled","mFakeRecipe"})s.append(f).append('=').append(canonical(field(r,f))).append('|');
     StringBuilder extra=new StringBuilder();Object metadata=call(method(r.getClass(),0,"getMetadataStorage"),r);
     if(metadata!=null)for(Object entry:(Set<?>)call(method(metadata.getClass(),0,"getEntries"),metadata)) {
      Object value=((Map.Entry<?,?>)entry).getValue();
      if(value!=null && type("gtnhintergalactic.recipe.SpaceMiningData").isInstance(value)) {
       for(String f:new String[]{"asteroidName","minDistance","maxDistance","minSize","maxSize","computation","recipeWeight"})extra.append("space:").append(f).append('=').append(canonical(field(value,f))).append('|');
      }
     }
     rows.add(s.toString()+extra);normalized.add(semanticRecipe(r)+"|"+extra);
    }
    p.println("machine,"+e.getKey()+","+rows.size()+","+hash(rows));recipes+=rows.size();
    p.println("machine_semantic,"+e.getKey()+","+normalized.size()+","+hash(normalized));
    if(Arrays.asList("cropsnh.recipes.cropBreeder","gt.recipe.centrifuge","gt.recipe.eyeofharmony","gt.recipe.fakeAssemblylineProcess","gt.recipe.multicentrifuge").contains(e.getKey().toString()))
     {
      try(PrintWriter details=new PrintWriter(new File(MiningWorldTests.output(),"parity-details-"+e.getKey()+".txt"))){for(String row:rows)details.println(Base64.getEncoder().encodeToString(row.getBytes("UTF-8")));}
      try(PrintWriter details=new PrintWriter(new File(MiningWorldTests.output(),"semantic-details-"+e.getKey()+".txt"))){for(String row:normalized)details.println(Base64.getEncoder().encodeToString(row.getBytes("UTF-8")));}
     }
   }
   Class<?> vc=type("bwcrossmod.galacticgreg.VoidMinerUtility");
   for(String table:new String[]{"dropMapsByDimName","extraDropsByDimName"}) {
    Map<?,?> vm=(Map<?,?>)vc.getField(table).get(null);
    for(Map.Entry<?,?> e:vm.entrySet()) {
     List<String> rows=new ArrayList<>();Map<?,?> weights=(Map<?,?>)call(method(e.getValue().getClass(),0,"getInternalMap"),e.getValue());
     for(Map.Entry<?,?> w:weights.entrySet()){Object stack=call(method(w.getKey().getClass(),1,"getItemStack"),w.getKey(),1);rows.add(item(stack)+"="+w.getValue());}
     p.println(table+","+e.getKey()+","+rows.size()+","+hash(rows));voidDims++;
    }
   }
   Object extreme=call(method(type("fox.spiteful.avaritia.crafting.ExtremeCraftingManager"),0,"getInstance"),null);
   for(Object r:(List<?>)call(method(extreme.getClass(),0,"getRecipeList"),extreme)) {
    Object output=call(method(r.getClass(),0,"getRecipeOutput","func_77571_b"),r);
    if(output==null)continue;String id=item(output);
    if(!id.startsWith("SGCraft:"))continue;
    Object input=call(method(r.getClass(),0,"getInput"),r);
    p.println("stargate,"+id.replace(',','_')+",1,"+hash(new ArrayList<>(Arrays.asList(canonical(input)))));sg++;
   }
   for(Map.Entry<String,Integer> e:placeholders.entrySet())p.println("placeholder,"+e.getKey().replace(',','_')+","+e.getValue()+","+hash(new ArrayList<>(Arrays.asList(e.getKey()))));
   List<String> assembly=new ArrayList<>();semantic=true;
   try {
    for(Object r:(List<?>)type("gregtech.api.util.GTRecipe$RecipeAssemblyLine").getField("sAssemblylineRecipes").get(null)) {
     StringBuilder s=new StringBuilder(inputs(r,"mInputs","mOreDictAlt",null));
     for(String f:new String[]{"mResearchItem","mResearchTime","mResearchVoltage","mFluidInputs","mOutput","mDuration","mEUt"})s.append('|').append(f).append('=').append(canonical(field(r,f)));
     assembly.add(s.toString());
    }
   } finally{semantic=false;}
   p.println("assemblyline,ordered-inputs-research-and-output,"+assembly.size()+","+hash(assembly));
  }
  helper.assertTrue(recipes>1000&&voidDims>20&&sg>=8,"Expected initialized recipes, void mining and Stargate crafting");
  System.out.println("[ClassicMiningQA] Progression parity exported "+recipes+" machine recipes, "+voidDims+" void tables, "+sg+" Stargate recipes. SpaceMiningData included; other auxiliary recipe metadata is not fingerprinted.");helper.succeed();
 }
}
