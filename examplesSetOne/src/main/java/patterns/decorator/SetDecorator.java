package patterns.decorator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SetDecorator<T> implements Set<T> {
	
	private Set<T> decoratee;
	
	public SetDecorator(Set<T> decoratee) {
		this.decoratee = decoratee;
	}

	public boolean add(T arg0) {
		return decoratee.add(arg0);
	}

	public boolean addAll(Collection<? extends T> arg0) {
		return decoratee.addAll(arg0);
	}

	public void clear() {
		decoratee.clear();
	}

	public boolean contains(Object arg0) {
		return decoratee.contains(arg0);
	}

	public boolean containsAll(Collection<?> arg0) {
		return decoratee.containsAll(arg0);
	}

	public boolean isEmpty() {
		return decoratee.isEmpty();
	}

	public Iterator<T> iterator() {
		/*
		 *  TODO: This one would actually be slightly more problematic.
		 *  Exercise for the reader -- why would this one cause a problem.
		 */
		return decoratee.iterator();
	}

	public boolean remove(Object arg0) {
		return decoratee.remove(arg0);
	}

	public boolean removeAll(Collection<?> arg0) {
		return decoratee.removeAll(arg0);
	}

	public boolean retainAll(Collection<?> arg0) {
		return decoratee.retainAll(arg0);
	}

	public int size() {
		return decoratee.size();
	}

	public Object[] toArray() {
		return decoratee.toArray();
	}

	public <U> U[] toArray(U[] arg0) {
		return decoratee.toArray(arg0);
	}
	
 
	

}
