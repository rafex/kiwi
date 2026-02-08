package dev.rafex.kiwi.handlers.dto;

public class MoveObjectRequest {

	private String newLocationId;

	public String getNewLocationId() {
		return newLocationId;
	}

	public void setNewLocationId(final String newLocationId) {
		this.newLocationId = newLocationId;
	}

	@Override
	public String toString() {
		return "MoveObjectRequest[newLocationId=" + newLocationId + "]";
	}

}
