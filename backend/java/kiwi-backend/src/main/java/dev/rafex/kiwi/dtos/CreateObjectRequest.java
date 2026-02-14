package dev.rafex.kiwi.dtos;

import java.util.List;
import java.util.Objects;

public class CreateObjectRequest {
	private String name;
	private String description;
	private String type;
	private List<String> tags;
	private Object metadata; // lo serializamos a json string
	private String locationId; // UUID en string

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(final List<String> tags) {
		this.tags = tags;
	}

	public Object getMetadata() {
		return metadata;
	}

	public void setMetadata(final Object metadata) {
		this.metadata = metadata;
	}

	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(final String locationId) {
		this.locationId = locationId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, locationId, metadata, name, tags, type);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		final var other = (CreateObjectRequest) obj;
		return Objects.equals(description, other.description) && Objects.equals(locationId, other.locationId)
				&& Objects.equals(metadata, other.metadata) && Objects.equals(name, other.name)
				&& Objects.equals(tags, other.tags) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "CreateObjectRequest [name=" + name + ", description=" + description + ", type=" + type + ", tags="
				+ tags + ", metadata=" + metadata + ", locationId=" + locationId + "]";
	}

}
