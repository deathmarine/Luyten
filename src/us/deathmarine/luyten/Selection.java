package us.deathmarine.luyten;

public class Selection implements Comparable<Selection> {
	public final Integer from;
	public final Integer to;

	public Selection(Integer from, Integer to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public int compareTo(Selection o) {
		return from.compareTo(o.from);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Selection other = (Selection) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		return true;
	}
}
