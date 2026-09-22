package com.storemanager.domain.category.model;

public class Category {

    private Long id;

    private String name;

    private boolean active;

    private String imagePath;

    public Category() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(
            String name
    ) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active
    ) {
        this.active = active;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(
            String imagePath
    ) {
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {

        if (id == null) {
            return name;
        }

        return id + " - " + name;
    }
}
