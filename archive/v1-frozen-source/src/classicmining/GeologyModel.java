package classicmining;

import java.util.*;

/** Gameplay-scale morphology, informed by deposit families rather than terrestrial ore grades. */
public final class GeologyModel {
    public enum Family { BED, STOCKWORK, LODE, LENS, PIPE, CRYSTAL, CORE, VEIN }
    private GeologyModel() {}
    public static Family family(String[] materials) {
        Set<String> m=new HashSet<>(Arrays.asList(materials));
        if(materials.length==4 && has(Collections.singleton(materials[3]),"Neutronium","Adamantium","Pentlandite","Cooperite"))return Family.LENS;
        if(has(m,"Mica","Cassiterite","CassiteriteSand","Tin","Tungstate","Scheelite"))return Family.LODE;
        if(has(m,"Naquadah","NaquadahEnriched","Naquadria"))return Family.CORE;
        if(has(m,"Neutronium","Adamantium","Pentlandite","Cooperite","Platinum","Palladium"))return Family.LENS;
        if(has(m,"Diamond"))return Family.PIPE;
        if(has(m,"MysteriousCrystal","Dilithium","Vinteum","Amethyst","Topaz","BlueTopaz"))return Family.CRYSTAL;
        if(has(m,"Chalcopyrite","Bornite","Malachite","Copper","Tetrahedrite"))return Family.STOCKWORK;
        if(has(m,"Magnetite","VanadiumMagnetite","Iron","BrownLimonite","YellowLimonite"))return Family.BED;
        return Family.VEIN;
    }
    private static boolean has(Set<String> values,String... names) {for(String n:names)if(values.contains(n))return true;return false;}
    public static int targetSize(Random rng,boolean mica) {
        // Same ordinary/seam probabilities as algorithm4. Half its jackpot mass funds industrial bodies.
        int roll=rng.nextInt(30000);
        if(roll<28200)return 6+rng.nextInt(mica?3:11);    // 94%, E=11 (mica=7)
        if(roll<29850)return 28+rng.nextInt(37);         // 5.5%, E=46
        if(roll<29925)return 160+rng.nextInt(161);       // 0.25%, E=240
        if(roll<29935)return 1200+rng.nextInt(1201);     // 1/3000, E=1800
        return 0;                                      // 65/30000 intentionally unused budget
    }
    public static double expectedSize(boolean mica) {return .94*(mica?7:11)+.055*46+.0025*240+1800.0/3000;}
    private static final int[][] DIRS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final class Node {
        int[] p; double score; long order;
        Node(int[] p,double score,long order){this.p=p;this.score=score;this.order=order;}
    }
    public static List<int[]> body(Random rng,int x,int y,int z,int lo,int hi,int target,Family family) {
        if(target<=0)return Collections.emptyList();
        int low=Math.max(lo,y-12),high=Math.min(hi,y+12);
        int count=Math.min(target,25*25*(high-low+1));
        int orientation=rng.nextInt(4); double jitter=rng.nextDouble();
        PriorityQueue<Node> frontier=new PriorityQueue<>(new Comparator<Node>(){public int compare(Node a,Node b){int c=Double.compare(a.score,b.score);return c==0?Long.compare(a.order,b.order):c;}});
        Set<String> seen=new HashSet<>(); List<int[]> result=new ArrayList<>();long serial=0;
        frontier.add(new Node(new int[]{x,y,z},0,serial++));seen.add(x+","+y+","+z);
        while(result.size()<count) {
            if(frontier.isEmpty())throw new IllegalStateException("Connected bounded body exhausted");
            int[] p=frontier.remove().p;result.add(p);
            for(int[] d:DIRS) {
                int nx=p[0]+d[0],ny=p[1]+d[1],nz=p[2]+d[2];
                if(Math.abs(nx-x)>12 || Math.abs(nz-z)>12 || ny<low || ny>high)continue;
                if(!seen.add(nx+","+ny+","+nz))continue;
                double a=nx-x,b=ny-y,c=nz-z;
                if((orientation&1)!=0){double t=a;a=c;c=t;}
                if((orientation&2)!=0)a=-a;
                double score;
                switch(family) {
                    case BED: score=a*a+c*c+20*Math.pow(b-.10*a,2);break;
                    case LODE: score=.5*a*a+2*b*b+12*Math.pow(c-.35*b,2);break;
                    case LENS: score=a*a+2*c*c+12*b*b;break;
                    case PIPE: score=8*(a*a+c*c)+.4*b*b;break;
                    case STOCKWORK:
                        double fracture=Math.min(Math.abs(b-.45*a),Math.min(Math.abs(b+.45*c),Math.abs(a-c)));
                        score=.8*(a*a+b*b+c*c)+18*fracture*fracture;break;
                    case CRYSTAL: score=a*a+b*b+c*c+6*Math.abs(a+b+c);break;
                    case CORE: score=a*a+1.6*b*b+c*c;break;
                    default: score=.6*a*a+5*b*b+4*c*c;
                }
                // Small deterministic irregularities, not independent mineral sprinkling.
                score*=.85+.30*rng.nextDouble();score+=jitter;
                frontier.add(new Node(new int[]{nx,ny,nz},score,serial++));
            }
        }
        return result;
    }
}
