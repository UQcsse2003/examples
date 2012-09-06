package patterns.decorator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class OldInstrumentedHashSet<T> extends HashSet<T> {
	
	
	private int addCount = 0;
	
	public int getAddCount() {
		return addCount;
	}
		
	@Override
	public boolean add(T e) {
		addCount++;
		return super.add(e);
	}
	
	@Override
	public boolean addAll(Collection<? extends T> c) {
		addCount += c.size();
		return super.addAll(c);
	}
	
	

}
