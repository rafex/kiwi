package dev.rafex.kiwi.dtos;

public class CreateLocationRequest {

	private String name;
	private String parentLocationId;

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getParentLocationId() {
		return parentLocationId;
	}

	public void setParentLocationId(final String parentLocationId) {
		this.parentLocationId = parentLocationId;
	}

	@Override
	public String toString() {
		return "CreateLocationRequest [name=" + name + ", parentLocationId=" + parentLocationId + "]";
	}
}
