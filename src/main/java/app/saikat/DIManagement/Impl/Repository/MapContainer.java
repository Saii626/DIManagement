package app.saikat.DIManagement.Impl.Repository;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

class MapContainer<K, V> extends Container<Map<K, V>, ImmutableMap<K, V>> {
	public MapContainer(Map<K, V> data) {
		super(data);
	}

	public ImmutableMap<K, V> getImmutable() {
		return super.getImmutable(ImmutableMap::copyOf);
	}

	public V put(K key, V val) {
		synchronized (this) {
			this.isModified.set(true);
			return data.put(key, val);
		}
	}

	public void putAll(Map<K, V> map) {
		synchronized (this) {
			this.isModified.set(true);
			data.putAll(map);
		}
	}
}