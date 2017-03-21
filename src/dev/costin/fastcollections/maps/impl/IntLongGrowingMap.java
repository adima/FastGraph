package dev.costin.fastcollections.maps.impl;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import dev.costin.fastcollections.IntIterator;
import dev.costin.fastcollections.maps.IntLongMap;
import dev.costin.fastcollections.tools.FastCollections;

public class IntLongGrowingMap implements IntLongMap {
   
   protected static class KeyIterator implements dev.costin.fastcollections.IntIterator {

      private final IntLongGrowingMap _map;

      private final Object[]       _list;

      private int               _next;

      private IntLongEntryImpl   _last;

      private int               _modCounter;
      private int               _lastRemoved;

      KeyIterator( final IntLongGrowingMap map ) {
         _map = map;
         _list = _map._entryList;
         _next = 0;
         _modCounter = _map._modCounter;
         _lastRemoved = -1;
      }

      @Override
      public int nextInt() {
         if( _modCounter != _map._modCounter ) {
            throw new ConcurrentModificationException();
         }

         return ( _last = (IntLongEntryImpl)_list[_next++] ).getKey();
      }

      @Override
      public boolean hasNext() {
         return _next < _map.size();
      }

      @Override
      public void remove() {
         if( _modCounter != _map._modCounter ) {
            throw new ConcurrentModificationException();
         }
         if( _lastRemoved >= _next ) {
            throw new NoSuchElementException();
         }
         // it is important to use the remove method of the set
         // to ensure that subclass of the set are still able to use
         // this iterator!
         _lastRemoved = --_next;
         _map.remove( _last );
         ++_modCounter;
      }

   }
   
   private static class EntryIterator implements Iterator<IntLongEntry> {
      private final IntLongGrowingMap _map;

      private final IntLongEntryImpl[]       _list;

      private int               _next;

      private IntLongEntryImpl   _lastEntry;

      private int               _modCounter;
      private int               _lastRemoved;

      EntryIterator( final IntLongGrowingMap map ) {
         _map = map;
         _list = _map._entryList;
         _next = 0;
         _modCounter = _map._modCounter;
         _lastRemoved = -1;
      }

      @Override
      public IntLongEntry next() {
         if( _modCounter != _map._modCounter ) {
            throw new ConcurrentModificationException();
         }

         return _lastEntry = _list[_next++];
      }

      @Override
      public boolean hasNext() {
         return _next < _map.size();
      }

      @Override
      public void remove() {
         if( _modCounter != _map._modCounter ) {
            throw new ConcurrentModificationException();
         }
         if( _lastRemoved >= _next ) {
            throw new NoSuchElementException();
         }
         // it is important to use the remove method of the set
         // to ensure that subclass of the set are still able to use
         // this iterator!
         _lastRemoved = --_next;
         _map.remove( _lastEntry );
         ++_modCounter;
      }
   }
   
   private static class IntLongEntryImpl implements IntLongEntry {
      private final int _key;
      private long _val;
      
      int _ref;
      
      IntLongEntryImpl( final int key, final long value, final int ref ) {
         _key = key;
         _val = value;
         _ref = ref;
      }

      @Override
      public int getKey() {
         return _key;
      }

      @Override
      public long getValue() {
         return _val;
      }

      @Override
      public void setValue( final long value ) {
         _val = value;
      }
      
   }
   
   private IntLongEntryImpl[] EMPTY = {};
   
   private IntLongEntryImpl[]     _keySet;
   private IntLongEntryImpl[]     _entryList;
   private int         _size;
   private int         _offset;
   protected int       _modCounter = 0;

   public IntLongGrowingMap() {
      this( FastCollections.DEFAULT_LIST_CAPACITY );
   }
   
   public IntLongGrowingMap( final IntLongMap map ) {
      if( map instanceof IntLongGrowingMap && map.size() > 0 ) {
         final IntLongGrowingMap gmap = (IntLongGrowingMap) map;
         thisInit( gmap );
      }
      else {
         init( 0, FastCollections.DEFAULT_LIST_CAPACITY-1, Math.max( map.size(), FastCollections.DEFAULT_LIST_CAPACITY ) );
      }
      
      for( IntLongEntry entry : map ) {
         put( entry.getKey(), entry.getValue() );
      }
   }

   public IntLongGrowingMap( final int n ) {
      this( 0, n - 1 );
   }

   public IntLongGrowingMap( final int from, final int to ) {
      this( from, to, Math.min( to - from + 1, FastCollections.DEFAULT_LIST_CAPACITY ) );
   }

   public IntLongGrowingMap( final int from, final int to, final int listCapacity ) {
      init( from, to, listCapacity );
   }
   
   private void init( final int from, final int to, final int listCapacity ) {
      _offset = from;
      _keySet = new IntLongEntryImpl[to - from + 1];
      _entryList = new IntLongEntryImpl[listCapacity];
      _size = 0;
   }
   
   private void thisInit( final IntLongGrowingMap map ) {
      final int mapOffset = map._offset;
      
      if( map.containsKey( mapOffset ) ) {
         final int lastKey = mapOffset + map.size() -1;

         if( map.containsKey( lastKey ) ) {
            init( mapOffset, lastKey, map.size() );
            return;
         }
         
         final int maxKey = mapOffset + map._keySet.length - 1;
         
         if( map.containsKey( maxKey ) ) {
            init( mapOffset, maxKey, map.size() );
            return;
         }
      }
      
      int min, max;
      final IntLongEntryImpl[] mapEntries = map._entryList;
      min = max = mapEntries[0].getKey();
      
      for( int i=1; i < map.size(); i++ ) {
         final int key = mapEntries[i].getKey();

         if( key < min ) {
            min = key;
         }
         else if( key > max ) {
            max = key;
         }
      }

      init( (min + mapOffset + 1) >> 1, (max + mapOffset + map._keySet.length) >> 1, map.size() );
   }

   @Override
   public boolean containsKey( final int key ) {
      if( key >= _offset ) {
         final int k = key - _offset;
         
         if( k<_keySet.length ) {
            final IntLongEntryImpl entry = _keySet[k];
            return entry != null && entry._ref >= 0;
         }
      }
      
      return false;
   }

   @Override
   public boolean put( final int key, final long value ) {
      ensureRangeFor( key );
      
      int k = key - _offset;
      
      final IntLongEntryImpl entry = _keySet[k];
      
      if( entry == null ) {
         _keySet[k] = addToList( key, value );
         ++_modCounter;
         
         return true;
      }
      else if( entry._ref < 0 ) {
         addToList( entry, value );
         ++_modCounter;
         
         return true;
      }
      else {
         entry.setValue( value );
         return false;
      }
   }

   @Override
   public boolean remove( final int key ) {
      if( key >= _offset ) {
         final int k = key - _offset;
         
         if( k < _keySet.length ) {
            final IntLongEntryImpl entry = _keySet[k];
            
            if( entry != null && entry._ref >= 0 ) {
               return remove( entry );
            }
         }
      }
      
      return false;
   }
   
   protected boolean remove( final IntLongEntryImpl entry ) {
      final int ref = entry._ref;
      assert( ref >= 0 );
      
      if( ref != --_size ) {
         (_entryList[ref] = _entryList[_size])._ref = ref;
      }
      entry._ref = -1;  // deleted

      ++_modCounter;

      return true;
   }

   @Override
   public long get( int key ) {
      if( key >= _offset ) {
         final int k = key - _offset;
         
         if( k < _keySet.length ) {
            final IntLongEntryImpl entry = _keySet[k];
            if( entry != null && entry._ref >= 0 ) {
               return entry.getValue();
            }
         }
      }
      // TODO: java-doc for this different behavior!
      throw new NoSuchElementException("Key "+key+" not found.");
   }

   @Override
   public int size() {
      return _size;
   }
   
   @Override
   public boolean isEmpty() {
      return _size == 0;
   }

   @Override
   public IntIterator keyIterator() {
      return new KeyIterator( this );
   }
   
   @Override
   public Iterator<IntLongEntry> iterator() {
      return new EntryIterator( this );
   }
   
   @Override
   public void clear() {
      for( int i = 0; i < _size; i++ ) {
         if( _entryList[i] != null ) {
            _entryList[i]._ref = -1;
         }
      }
      _size = 0;
      ++_modCounter;
   }
   
   @Override
   public boolean equals( final Object obj ) {
      if( obj instanceof IntLongMap ) {
         final IntLongMap map = (IntLongMap) obj;
         
         if( this != map && map.size() == size() ) {
            for( IntLongEntry entry : map ) {
               if( ! containsKey( entry.getKey() ) || entry.getValue() != _keySet[ entry.getKey() - _offset ].getValue() ) {
                  return false;
               }
            }
         }
         return true;
      }
      return false;
   }

   private IntLongEntryImpl addToList( final int key, final long value ) {
      ensureListCapacity( _size + 1 );
      
      final IntLongEntryImpl entry = new IntLongEntryImpl( key, value, _size );
      _entryList[_size++] = entry;
      return entry;
   }

   private void addToList( final IntLongEntryImpl entry, final long value ) {
      ensureListCapacity( _size + 1 );
      
      entry._ref = _size;
      entry._val = value;
      _entryList[_size++] = entry;
   }

   private void ensureRangeFor( final int key ) {
      if( _keySet == EMPTY ) {
         _keySet = new IntLongEntryImpl[ FastCollections.DEFAULT_LIST_CAPACITY ];
         _offset = key - ( FastCollections.DEFAULT_LIST_CAPACITY >>> 1 );
      }
      else {
         final int v = key - _offset;
         if( v < 0 ) {
            growNegative(
                  capacity( _keySet.length - v + ( _keySet.length >>> 1 ) )
                  - _keySet.length );
         }
         else if( v >= _keySet.length ) {
            growPositive(
                  capacity( v + 1 + ( _keySet.length >>> 1 ) )
                  - _keySet.length );
         }
      }
   }
   
   private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
   
   private int capacity( final int minCapacity ) {
      if( minCapacity < 0 ) { // overflow
         throw new OutOfMemoryError();
      }
      final int oldCapacity = _keySet.length;
      int newCapacity = oldCapacity + (oldCapacity >>> 2);
      if( newCapacity - minCapacity < 0 ) {
         newCapacity = minCapacity;
      }
      if( newCapacity - MAX_ARRAY_SIZE > 0 ) {
         newCapacity = hugeCapacity(minCapacity);
      }
      
      return newCapacity;
   }
   
   private static int hugeCapacity(final int minCapacity) {
      if( minCapacity < 0 ) { // overflow
         throw new OutOfMemoryError();
      }
      return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
   }
   
   private void growPositive( int count ) {
      _keySet = Arrays.copyOf( _keySet, _keySet.length + count );
   }
   
   private void growNegative( int count ) {
      final IntLongEntryImpl[] _newSet = new IntLongEntryImpl[ _keySet.length + count ];
      System.arraycopy( _keySet, 0, _newSet, count, _keySet.length );
      _keySet = _newSet;
      _offset -= count;
   }

   private void ensureListCapacity( final int minCapacity ) {
      if( minCapacity < 0 ) { // overflow
         throw new OutOfMemoryError();
      }
      if( _entryList == EMPTY ) {
         _entryList = new IntLongEntryImpl[ Math.max( minCapacity, FastCollections.DEFAULT_LIST_CAPACITY ) ];
      }
      else if( minCapacity > _entryList.length ) {
         final int maxDelta = _keySet.length - _entryList.length;

         int growDelta = 1 + ( _entryList.length >> 1 );
         if( growDelta > maxDelta ) {
            growDelta = maxDelta;
         }
         // minCapacity can never be > _set.length !
         else if( minCapacity - _entryList.length > growDelta ) {
            growDelta = minCapacity - _entryList.length;
         }
         
         _entryList = Arrays.copyOf( _entryList, _entryList.length + growDelta );
      }
   }
}
