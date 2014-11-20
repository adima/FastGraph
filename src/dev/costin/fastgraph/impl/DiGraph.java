package dev.costin.fastgraph.impl;

import dev.costin.fastcollections.sets.IntSet;
import dev.costin.fastcollections.sets.impl.IntRangeSet;
import dev.costin.fastgraph.Adjacency;
import dev.costin.fastgraph.Graph;

public class DiGraph implements Graph {

   private IntSetAdjacency[] _graph;
   private int[]             _inDegree;

   public static class IntSetAdjacency extends IntRangeSet implements Adjacency {

      private final DiGraph _ownerGraph;
      private final int     _owner;

      protected IntSetAdjacency( final DiGraph ownerGraph, final int owner ) {
         this( ownerGraph, owner, ownerGraph.verticesCount() );
      }

      protected IntSetAdjacency( final DiGraph ownerGraph, final int owner, final int initialListCapacity ) {
         super( 0, ownerGraph.verticesCount() - 1, initialListCapacity );
         _ownerGraph = ownerGraph;
         _owner = owner;
      }

      @Override
      public Graph ownerGraph() {
         return _ownerGraph;
      }

      @Override
      public int owner() {
         return _owner;
      }

      @Override
      public boolean add( int vertex ) {
         if( super.add( vertex ) ) {
            ++_ownerGraph._inDegree[vertex];
            return true;
         }
         return false;
      }

      @Override
      public boolean remove( int vertex ) {
         if( super.remove( vertex ) ) {
            --_ownerGraph._inDegree[vertex];
            return true;
         }
         return false;
      }
      
      @Override
      public void clear() {
         for( int i=0; i<size(); i++ ) {
            --_ownerGraph._inDegree[get(i)];
         }
         super.clear();
      }
   }

   public DiGraph( final int n ) {
      _graph = new IntSetAdjacency[n];
      for( int i = 0; i < n; i++ ) {
         _graph[i] = createAdjacency( this, i );
      }

      _inDegree = new int[n];
   }

   /**
    * Constructor that does nothing. Used for fast construction of subgraphs
    * where the internal structure are created as needed.
    */
   protected DiGraph() {

   }

   @Override
   public int verticesCount() {
      return _graph.length;
   }

   @Override
   public Adjacency adjacencyOf( int vertex ) {
      return _graph[vertex];
   }

   @Override
   public boolean hasEdge( int source, int dest ) {
      return adjacencyOf( source ).contains( dest );
   }

   @Override
   public boolean addEdge( int source, int dest ) {
      return adjacencyOf( source ).add( dest );
   }

   @Override
   public boolean removeEdge( int source, int dest ) {
      return adjacencyOf( source ).remove( dest );
   }

   @Override
   public int getOutDegree( int vertex ) {
      return adjacencyOf( vertex ).size();
   }

   @Override
   public int getInDegree( int vertex ) {
      return _inDegree[vertex];
   }

   @Override
   public Graph subGraph( IntSet vertices ) {
      final DiGraph subGraph = new DiGraph();
      // TODO: maybe limit the range to minimal needed?
      subGraph._graph = new IntSetAdjacency[_graph.length];
      subGraph._inDegree = new int[_graph.length];
      
      final int subGraphVerticesCount = vertices.size();

      for( int i = 0; i < _graph.length; i++ ) {
         if( vertices.contains( i ) ) {
            final IntSetAdjacency adj = _graph[i];
            
            if( adj != null ) {
               final IntSetAdjacency newAdj =
                     subGraph._graph[i] = createAdjacency( subGraph, i, Math.min(adj.size(), subGraphVerticesCount) );
               
               for( int j=0; j<adj.size(); j++ ) {
                  final int v = adj.get( j );
                  if( vertices.contains( v ) ) {
                     newAdj.add( v );
                  }
               }
            }
         }
      }

      return subGraph;
   }

   protected IntSetAdjacency createAdjacency( final DiGraph ownerGraph, final int owner ) {
      return new IntSetAdjacency( ownerGraph, owner );
   }

   protected IntSetAdjacency createAdjacency( final DiGraph ownerGraph, final int owner, final int initialCapacity ) {
      return new IntSetAdjacency( ownerGraph, owner, initialCapacity );
   }
}