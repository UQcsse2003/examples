package week8.village;

import java.util.HashSet;
import java.util.Set;

import week8.Person;


public enum Village {
	
	INSTANCE;
	
	private Set<Person> occupants = new HashSet<Person>();
	
	public void enter(Person p) {
		occupants.add(p);
	}
		
	public void leave(Person p) {
		throw new UnsupportedOperationException("Be seeing you.");
	}
	
	public Set<Person> getOccupants() {
		// There is an escape route hidden in this code...
		return this.occupants;
	}
	
	/**
	 * For calling at the beginning of tests
	 */
	void clear() {
		occupants.clear();
	}

}
