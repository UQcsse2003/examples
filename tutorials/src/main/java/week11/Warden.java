package week11;

import java.util.*;

import week11.village.Village;

public class Warden extends Person {

	/**
	 * Oppress a villager 
	 */
	public void torture(Person victim, List<? extends Person> bystanders) {
		List<Person> allPresent = new ArrayList<Person>();
		allPresent.addAll(bystanders);
		allPresent.add(this);
		
		victim.torture(allPresent);
		for (Person p : bystanders) {
			p.observedTorture(victim);
		}		
	}
	
	public void torture(Warden victim, List<? extends Person> bystanders) {
		// Do nothing		
	}
	
	public Response escapeWithMe(Person p) {
		// Warden's always report the request
		return Village.INSTANCE.reportConspiracy(p);
	}	
}
