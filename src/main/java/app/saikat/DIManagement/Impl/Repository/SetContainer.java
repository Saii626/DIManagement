package app.saikat.DIManagement.Impl.Repository;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

class SetContainer<T> extends Container<Set<T>, ImmutableSet<T>> {
	public SetContainer(Set<T> data) {
		super(data);
	}

	public ImmutableSet<T> getImmutable() {
		return super.getImmutable(ImmutableSet::copyOf);
	}

	public boolean add(T t) {
		synchronized (this) {
			this.isModified.set(true);
			return data.add(t);
		}
	}

	public boolean addAll(Collection<T> coll) {
		synchronized (this) {
			this.isModified.set(true);
			return data.addAll(coll);
		}
	}
}