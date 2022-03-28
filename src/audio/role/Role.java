package audio.role;

import java.util.BitSet;

public class Role {

	public final int index;	
	public final String name;	
	public final Role[] roles;

	public Role(int index, String name) {
		this.index = index;
		this.name = name;
		this.roles = null;
	}

	public Role(int index, String name, Role[] roles) {
		this.index = index;
		this.name = name;
		this.roles = roles;
	}

	public boolean has(BitSet roleBits) {
		return roleBits.get(index);
	}

	public void checkHas(BitSet roleBits) {
		if(roleBits == null) {
			throw new RuntimeException("could not check role because roleBits not available: " + name);
		}
		if(!roleBits.get(index)) {
			throw new RuntimeException("role not included in account: " + name);
		}
	}
	
	public void checkHasNot(BitSet roleBits) {
		if(roleBits == null) {
			throw new RuntimeException("could not check role because roleBits not available: " + name);
		}
		if(roleBits.get(index)) {
			throw new RuntimeException("role included in account: " + name);
		}
	}
	
	public void populateSelf(BitSet bitSet) {
		bitSet.set(index);		
	}

	public void populateRecursive(BitSet bitSet) {
		populateSelf(bitSet);
		if(roles != null) {
			for(Role role:roles) {
				role.populateRecursive(bitSet);
			}
		}
	}
}
