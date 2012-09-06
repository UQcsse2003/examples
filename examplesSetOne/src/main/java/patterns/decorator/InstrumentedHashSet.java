package patterns.decorator;

import java.util.Collection;
import java.util.Set;

public class InstrumentedHashSet<T> extends SetDecorator<T> {

	private int addCount = 0;
	
	public int getAddCount() {
		return addCount;
	}
	
	public InstrumentedHashSet(Set<T> decoratee) {
		super(decoratee);
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
