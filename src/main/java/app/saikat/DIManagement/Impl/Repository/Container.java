package app.saikat.DIManagement.Impl.Repository;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class Container<DATA, IMMUT_DATA> {
	protected DATA data;
	protected AtomicBoolean isModified;
	protected IMMUT_DATA immutableData;

	public Container(DATA data) {
		this.data = data;
		this.isModified = new AtomicBoolean(true);
		this.immutableData = null;
	}

	public IMMUT_DATA getImmutable(Function<DATA, IMMUT_DATA> converter) {
		if (this.isModified.get()) {
			synchronized (this) {
				if (this.isModified.get()) {
					this.immutableData = converter.apply(this.data);
					this.isModified.set(false);
				}
				return this.immutableData;
			}
		}
		return this.immutableData;
	}
}
