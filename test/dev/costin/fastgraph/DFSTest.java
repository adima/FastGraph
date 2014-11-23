package dev.costin.fastgraph;

import dev.costin.fastgraph.algos.DepthFirstSearch;
import dev.costin.fastgraph.algos.DepthFirstSearch.DFSVertexVisitor;
import dev.costin.fastgraph.algos.VertexVisitor;


public class DFSTest {
   
   static class MutableLong {
      long value;
   }

   static void testSimpleDFS( final Graph graph, final int count ) {
      final DepthFirstSearch dfs = new DepthFirstSearch();
      final MutableLong value = new MutableLong();
      final VertexVisitor visitor = new VertexVisitor() {
         
         @Override
         public boolean visit( int vertex ) {
            value.value++;
            return true;
         }
      };
      
      long start = System.currentTimeMillis();
      
      for( int i=0; i<count; i++ ) {
         dfs.traverse( graph, visitor );
      }
      
      System.out.println("time: "+(System.currentTimeMillis()-start));
   }

   static void testExtendedDFS( final Graph graph, final int count ) {
      final DepthFirstSearch dfs = new DepthFirstSearch();
      final MutableLong value = new MutableLong();
      final DFSVertexVisitor visitor = new DFSVertexVisitor() {
         
         @Override
         public boolean visit( int vertex ) {
            value.value++;
            return true;
         }
         
         @Override
         public boolean visitEdge( int source, int dest ) {
            return true;
         }

         @Override
         public void onNewTree( int root ) {
            
         }

         @Override
         public boolean onBackEdge( int source, int dest ) {
            return true;
         }

         @Override
         public boolean onTreeCrossingEdge( int source, int dest ) {
            return true;
         }

         @Override
         public boolean onSameTreeCrossingEdge( int source, int dest ) {
            return true;
         }

      };
      
      long start = System.currentTimeMillis();
      
      for( int i=0; i<count; i++ ) {
         dfs.traverse2( graph, visitor );
      }
      
      System.out.println("time: "+(System.currentTimeMillis()-start));
   }

   public static void main( String[] args ) {
      final int n = 1000;
      final Graph graph = Test.buildRandomGraph( n, n*8 );
      
      final int count = 1000;
      
//      testSimpleDFS( graph, count );
//      testSimpleDFS( graph, count );
//      testSimpleDFS( graph, count );
//      testSimpleDFS( graph, count );
//      testSimpleDFS( graph, count );
      
      testExtendedDFS( graph, count );
      testExtendedDFS( graph, count );
      testExtendedDFS( graph, count );
      testExtendedDFS( graph, count );
      testExtendedDFS( graph, count );
   }

}
