package nl.thedutchmc.muce.types;

public class Pair<T, K> {

	private final T m_first;
	private final K m_second;
	
	public Pair(T m_first, K m_second) {
		this.m_first = m_first;
		this.m_second = m_second;
	}
	
	public T getFirst() {
		return this.m_first;
	}
	
	public K getSecond() {
		return this.m_second;
	}
	
}
