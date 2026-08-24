package pt.glsmanagement.platform.entity;

class EntityNotFoundException extends RuntimeException {
    EntityNotFoundException() { super("Entity not found"); }
}
