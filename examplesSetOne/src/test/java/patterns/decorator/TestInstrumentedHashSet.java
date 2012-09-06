package patterns.decorator;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.*;

public class TestInstrumentedHashSet {

	@Test
	public void theTest() {
		
		InstrumentedHashSet<String> s = new InstrumentedHashSet<String>(new HashSet<String>());
		
		s.addAll(Arrays.asList("Snap", "Crackle", "Pop"));
		assertEquals("Whoops, the count was wrong", 3, s.getAddCount());
		
		
		
	}
	
}
