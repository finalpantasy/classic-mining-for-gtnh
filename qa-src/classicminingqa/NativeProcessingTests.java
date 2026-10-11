package classicminingqa;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.*;
import java.io.*;import java.util.*;
import static classicmining.Reflection.*;
@GameTestHolder("classicminingqa")
public class NativeProcessingTests {
 @GameTest(timeoutTicks=200,batch="classicmining_processing")
 public static void furnacePrimaryYields(GameTestHelper helper)throws Exception {
  String[] labels=System.getProperty("classicmining.qa.processingLabels","").split(",");
  Object furnace=call(method(type("net.minecraft.item.crafting.FurnaceRecipes"),0,"smelting","func_77602_a"),null);
  File reportRoot=MiningWorldTests.output().getParentFile();int cases=0;
  try(PrintWriter csv=new PrintWriter(new File(MiningWorldTests.output(),"native-furnace-yields.csv"))){
   csv.println("label,input,inputCount,furnaceOutput,outputPerInput,totalOutput");
   for(String label:labels) {
    if(!label.matches("[a-zA-Z0-9-]+"))throw new IllegalStateException("Invalid local report label");
    Map<String,Long> totals=new TreeMap<>();
    try(BufferedReader reader=new BufferedReader(new FileReader(new File(reportRoot,label+"/drill-drops.csv")))){
     reader.readLine();String line;
     while((line=reader.readLine())!=null){String[] v=line.split(",");String id=v[0];int count=Integer.parseInt(v[1]);String[] parts=id.split(":");
      if(parts.length!=2){csv.println(label+","+id+","+count+",unmapped,0,0");continue;}
      Object prefix=type("gregtech.api.enums.OrePrefixes").getField(parts[0]).get(null);
      Object material=parts[1];
      Object item=type("gregtech.api.util.GTOreDictUnificator").getMethod("get",type("gregtech.api.enums.OrePrefixes"),Object.class,long.class).invoke(null,prefix,material,1L);
      if(item==null)throw new IllegalStateException("Could not reconstruct native drop "+id);
      Object result=call(method(furnace.getClass(),1,"getSmeltingResult","func_151395_a"),furnace,item);cases++;
      String output=result==null?"noFurnaceRecipe":NativeMiningTests.identity(result);int units=NativeMiningTests.size(result);long total=(long)count*units;
      csv.println(label+","+id+","+count+","+output+","+units+","+total);
      if(result!=null)totals.put(output,totals.getOrDefault(output,0L)+total);
     }
    }
    NativeMiningTests.table(new File(MiningWorldTests.output(),label+"-furnace-totals.csv"),totals);
   }
  }
  helper.assertTrue(cases>0,"Native recipes must be queried for actual drill outputs");
  System.out.println("[ClassicMiningQA] Native furnace recipe outputs queried for "+cases+" actual-output cases. Advanced processing/byproducts excluded.");helper.succeed();
 }
}
