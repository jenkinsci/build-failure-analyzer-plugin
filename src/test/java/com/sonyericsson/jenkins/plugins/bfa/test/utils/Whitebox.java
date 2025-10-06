/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sonyericsson.jenkins.plugins.bfa.test.utils;
//CHECKSTYLE:OFF
import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Various utilities for accessing internals of a class. Basically a simplified
 * reflection utility intended for tests.
 * Borrowed from https://github.com/powermock/powermock/blob/e82e7f27dee1fe8c73bbce447774607c399ab261/powermock-reflect/src/main/java/org/powermock/reflect/Whitebox.java
 */
public class Whitebox {
    /**
     * Set the value of a field using reflection. This method will traverse the
     * super class hierarchy until a field with name <tt>fieldName</tt> is
     * found.
     *
     * @param object    the object whose field to modify
     * @param fieldName the name of the field
     * @param value     the new value of the field
     */
    public static void setInternalState(Object object, String fieldName, Object value) {
        WhiteboxImpl.setInternalState(object, fieldName, value);
    }

    /**
     * Set an array value of a field using reflection. This method will traverse
     * the super class hierarchy until a field with name <tt>fieldName</tt> is
     * found.
     *
     * @param object    the object to modify
     * @param fieldName the name of the field
     * @param value     the new value of the field
     */
    public static void setInternalState(Object object, String fieldName, Object[] value) {
        WhiteboxImpl.setInternalState(object, fieldName, value);
    }

    /**
     * Set the value of a field using reflection. This method will traverse the
     * super class hierarchy until the first field assignable to the
     * <tt>value</tt> type is found. The <tt>value</tt> (or
     * <tt>additionaValues</tt> if present) will then be assigned to this field.
     *
     * @param object           the object to modify
     * @param value            the new value of the field
     * @param additionalValues Additional values to set on the object
     */
    public static void setInternalState(Object object, Object value, Object... additionalValues) {
        WhiteboxImpl.setInternalState(object, value, additionalValues);
    }

    /**
     * Set the value of a field using reflection at at specific place in the
     * class hierarchy (<tt>where</tt>). This first field assignable to
     * <tt>object</tt> will then be set to <tt>value</tt>.
     *
     * @param object the object to modify
     * @param value  the new value of the field
     * @param where  the class in the hierarchy where the field is defined
     */
    public static void setInternalState(Object object, Object value, Class<?> where) {
        WhiteboxImpl.setInternalState(object, value, where);
    }

    /**
     * Set the value of a field using reflection. Use this method when you need
     * to specify in which class the field is declared. This might be useful
     * when you have mocked the instance you are trying to modify.
     *
     * @param object    the object to modify
     * @param fieldName the name of the field
     * @param value     the new value of the field
     * @param where     the class in the hierarchy where the field is defined
     */
    public static void setInternalState(Object object, String fieldName, Object value, Class<?> where) {
        WhiteboxImpl.setInternalState(object, fieldName, value, where);
    }

    /**
     * Set the value of a field using reflection. This method will traverse the
     * super class hierarchy until the first field of type <tt>fieldType</tt> is
     * found. The <tt>value</tt> will then be assigned to this field.
     *
     * @param object    the object to modify
     * @param fieldType the type of the field
     * @param value     the new value of the field
     */
    public static void setInternalState(Object object, Class<?> fieldType, Object value) {
        WhiteboxImpl.setInternalState(object, fieldType, value);
    }

    /**
     * Set the value of a field using reflection at a specific location (
     * <tt>where</tt>) in the class hierarchy. The <tt>value</tt> will then be
     * assigned to this field. The first field matching the <tt>fieldType</tt>
     * in the hierarchy will be set.
     *
     * @param object    the object to modify
     * @param fieldType the type of the field the should be set.
     * @param value     the new value of the field
     * @param where     which class in the hierarchy defining the field
     */
    public static void setInternalState(Object object, Class<?> fieldType, Object value, Class<?> where) {
        WhiteboxImpl.setInternalState(object, fieldType, value, where);
    }

    /**
     * Get the value of a field using reflection. This method will iterate
     * through the entire class hierarchy and return the value of the first
     * field named <tt>fieldName</tt>. If you want to get a specific field value
     * at specific place in the class hierarchy please refer to
     * {@link #getInternalState(Object, String, Class)}.
     *
     * @param object    the object to modify
     * @param fieldName the name of the field
     */
    public static <T> T getInternalState(Object object, String fieldName) {
        return WhiteboxImpl.getInternalState(object, fieldName);
    }

    /**
     * Get the value of a field using reflection. Use this method when you need
     * to specify in which class the field is declared. This might be useful
     * when you have mocked the instance you are trying to access.
     *
     * @param object    the object to modify
     * @param fieldName the name of the field
     * @param where     which class the field is defined
     */
    public static <T> T getInternalState(Object object, String fieldName, Class<?> where) {
        return WhiteboxImpl.getInternalState(object, fieldName, where);
    }

    /**
     * Get the value of a field using reflection. Use this method when you need
     * to specify in which class the field is declared. This might be useful
     * when you have mocked the instance you are trying to access. Use this
     * method to avoid casting.
     *
     * @param <T>       the expected type of the field
     * @param object    the object to modify
     * @param fieldName the name of the field
     * @param where     which class the field is defined
     * @param type      the expected type of the field
     * @deprecated Use {@link #getInternalState(Object, String, Class)} instead.
     */
    @Deprecated
    public static <T> T getInternalState(Object object, String fieldName, Class<?> where, Class<T> type) {
        return Whitebox.getInternalState(object, fieldName, where);
    }

    /**
     * Get the value of a field using reflection based on the fields type. This
     * method will traverse the super class hierarchy until the first field of
     * type <tt>fieldType</tt> is found. The value of this field will be
     * returned.
     *
     * @param object    the object to modify
     * @param fieldType the type of the field
     */
    public static <T> T getInternalState(Object object, Class<T> fieldType) {
        return WhiteboxImpl.getInternalState(object, fieldType);

    }

    /**
     * Get the value of a field using reflection based on the field type. Use
     * this method when you need to specify in which class the field is
     * declared. The first field matching the <tt>fieldType</tt> in
     * <tt>where</tt> is the field whose value will be returned.
     *
     * @param <T>       the expected type of the field
     * @param object    the object to modify
     * @param fieldType the type of the field
     * @param where     which class the field is defined
     */
    public static <T> T getInternalState(Object object, Class<T> fieldType, Class<?> where) {
        return WhiteboxImpl.getInternalState(object, fieldType, where);
    }

    static class WhiteboxImpl {
        /**
         * Set the value of a field using reflection. This method will traverse the
         * super class hierarchy until a field with name <tt>fieldName</tt> is
         * found.
         *
         * @param object    the object whose field to modify
         * @param fieldName the name of the field
         * @param value     the new value of the field
         */
        public static void setInternalState(Object object, String fieldName, Object value) {
            Field foundField = findFieldInHierarchy(object, fieldName);
            setField(object, value, foundField);
        }

        /**
         * Set the value of a field using reflection. This method will traverse the
         * super class hierarchy until a field with name <tt>fieldName</tt> is
         * found.
         *
         * @param object    the object to modify
         * @param fieldName the name of the field
         * @param value     the new value of the field
         */
        public static void setInternalState(Object object, String fieldName, Object[] value) {
            setInternalState(object, fieldName, (Object) value);
        }

        /**
         * Set the value of a field using reflection. This method will traverse the
         * super class hierarchy until the first field of type <tt>fieldType</tt> is
         * found. The <tt>value</tt> will then be assigned to this field.
         *
         * @param object    the object to modify
         * @param fieldType the type of the field
         * @param value     the new value of the field
         */
        public static void setInternalState(Object object, Class<?> fieldType, Object value) {
            setField(object, value, findFieldInHierarchy(object, new AssignableFromFieldTypeMatcherStrategy(fieldType)));
        }

        /**
         * Set the value of a field using reflection. This method will traverse the
         * super class hierarchy until the first field assignable to the
         * <tt>value</tt> type is found. The <tt>value</tt> (or
         * <tt>additionaValues</tt> if present) will then be assigned to this field.
         *
         * @param object           the object to modify
         * @param value            the new value of the field
         * @param additionalValues Additional values to set on the object
         */
        public static void setInternalState(Object object, Object value, Object... additionalValues) {
            setField(object, value,
                    findFieldInHierarchy(object, new AssignableFromFieldTypeMatcherStrategy(getType(value))));
            if (additionalValues != null) {
                for (Object additionalValue : additionalValues) {
                    setField(
                            object,
                            additionalValue,
                            findFieldInHierarchy(object, new AssignableFromFieldTypeMatcherStrategy(
                                    getType(additionalValue))));
                }
            }
        }

        /**
         * Set the value of a field using reflection at at specific place in the
         * class hierarchy (<tt>where</tt>). This first field assignable to
         * <tt>object</tt> will then be set to <tt>value</tt>.
         *
         * @param object the object to modify
         * @param value  the new value of the field
         * @param where  the class in the hierarchy where the field is defined
         */
        public static void setInternalState(Object object, Object value, Class<?> where) {
            setField(object, value,
                    findField(object, new AssignableFromFieldTypeMatcherStrategy(getType(value)), where));
        }

        /**
         * Set the value of a field using reflection at a specific location (
         * <tt>where</tt>) in the class hierarchy. The <tt>value</tt> will then be
         * assigned to this field.
         *
         * @param object    the object to modify
         * @param fieldType the type of the field the should be set.
         * @param value     the new value of the field
         * @param where     which class in the hierarchy defining the field
         */
        public static void setInternalState(Object object, Class<?> fieldType, Object value, Class<?> where) {
            if (fieldType == null || where == null) {
                throw new IllegalArgumentException("fieldType and where cannot be null");
            }

            setField(object, value, findFieldOrThrowException(fieldType, where));
        }

        /**
         * Set the value of a field using reflection. Use this method when you need
         * to specify in which class the field is declared. This is useful if you
         * have two fields in a class hierarchy that has the same name but you like
         * to modify the latter.
         *
         * @param object    the object to modify
         * @param fieldName the name of the field
         * @param value     the new value of the field
         * @param where     which class the field is defined
         */
        public static void setInternalState(Object object, String fieldName, Object value, Class<?> where) {
            if (object == null || fieldName == null || fieldName.isEmpty() || fieldName.startsWith(" ")) {
                throw new IllegalArgumentException("object, field name, and \"where\" must not be empty or null.");
            }

            final Field field = getField(fieldName, where);
            try {
                field.set(object, value);
            } catch (Exception e) {
                throw new RuntimeException("Internal Error: Failed to set field in method setInternalState.", e);
            }
        }

        /**
         * Get the value of a field using reflection. This method will iterate
         * through the entire class hierarchy and return the value of the first
         * field named <tt>fieldName</tt>. If you want to get a specific field value
         * at specific place in the class hierarchy please refer to
         *
         * @param <T>       the generic type
         * @param object    the object to modify
         * @param fieldName the name of the field
         * @return the internal state
         * {@link #getInternalState(Object, String, Class)}.
         */
        @SuppressWarnings("unchecked")
        public static <T> T getInternalState(Object object, String fieldName) {
            Field foundField = findFieldInHierarchy(object, fieldName);
            try {
                return (T) foundField.get(object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
            }
        }

        /**
         * Get the value of a field using reflection. This method will traverse the
         * super class hierarchy until the first field of type <tt>fieldType</tt> is
         * found. The value of this field will be returned.
         *
         * @param <T>       the generic type
         * @param object    the object to modify
         * @param fieldType the type of the field
         * @return the internal state
         */
        @SuppressWarnings("unchecked")
        public static <T> T getInternalState(Object object, Class<T> fieldType) {
            Field foundField = findFieldInHierarchy(object, new AssignableToFieldTypeMatcherStrategy(fieldType));
            try {
                return (T) foundField.get(object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
            }
        }

        /**
         * Get the value of a field using reflection. Use this method when you need
         * to specify in which class the field is declared. The first field matching
         * the <tt>fieldType</tt> in <tt>where</tt> will is the field whose value
         * will be returned.
         *
         * @param <T>       the expected type of the field
         * @param object    the object to modify
         * @param fieldType the type of the field
         * @param where     which class the field is defined
         * @return the internal state
         */
        @SuppressWarnings("unchecked")
        public static <T> T getInternalState(Object object, Class<T> fieldType, Class<?> where) {
            if (object == null) {
                throw new IllegalArgumentException("object and type are not allowed to be null");
            }

            try {
                return (T) findFieldOrThrowException(fieldType, where).get(object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
            }
        }

        /**
         * Get the value of a field using reflection. Use this method when you need
         * to specify in which class the field is declared. This might be useful
         * when you have mocked the instance you are trying to access. Use this
         * method to avoid casting.
         *
         * @param <T>       the expected type of the field
         * @param object    the object to modify
         * @param fieldName the name of the field
         * @param where     which class the field is defined
         * @return the internal state
         */
        @SuppressWarnings("unchecked")
        public static <T> T getInternalState(Object object, String fieldName, Class<?> where) {
            if (object == null || fieldName == null || fieldName.isEmpty() || fieldName.startsWith(" ")) {
                throw new IllegalArgumentException("object, field name, and \"where\" must not be empty or null.");
            }

            Field field = null;
            try {
                field = where.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(object);
            } catch (NoSuchFieldException e) {
                throw new FieldNotFoundException("Field '" + fieldName + "' was not found in class " + where.getName()
                        + ".");
            } catch (Exception e) {
                throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
            }
        }

        /**
         * Find field in hierarchy.
         *
         * @param object    the object
         * @param fieldName the field name
         * @return the field
         */
        private static Field findFieldInHierarchy(Object object, String fieldName) {
            return findFieldInHierarchy(object, new FieldNameMatcherStrategy(fieldName));
        }

        /**
         * Find field.
         *
         * @param object   the object
         * @param strategy the strategy
         * @param where    the where
         * @return the field
         */
        private static Field findField(Object object, FieldMatcherStrategy strategy, Class<?> where) {
            return findSingleFieldUsingStrategy(strategy, object, false, where);
        }


        /**
         * Find field in hierarchy.
         *
         * @param object   the object
         * @param strategy the strategy
         * @return the field
         */
        private static Field findFieldInHierarchy(Object object, FieldMatcherStrategy strategy) {
            assertObjectInGetInternalStateIsNotNull(object);
            return findSingleFieldUsingStrategy(strategy, object, true, getType(object));
        }

        /**
         * Find single field using strategy.
         *
         * @param strategy       the strategy
         * @param object         the object
         * @param checkHierarchy the check hierarchy
         * @param startClass     the start class
         * @return the field
         */
        private static Field findSingleFieldUsingStrategy(FieldMatcherStrategy strategy, Object object,
                                                          boolean checkHierarchy, Class<?> startClass) {
            assertObjectInGetInternalStateIsNotNull(object);
            Field foundField = null;
            final Class<?> originalStartClass = startClass;
            while (startClass != null) {
                final Field[] declaredFields = startClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    if (strategy.matches(field) && hasFieldProperModifier(object, field)) {
                        if (foundField != null) {
                            throw new TooManyFieldsFoundException("Two or more fields matching " + strategy + ".");
                        }
                        foundField = field;
                    }
                }
                if (foundField != null) {
                    break;
                } else if (!checkHierarchy) {
                    break;
                }
                startClass = startClass.getSuperclass();
            }
            if (foundField == null) {
                strategy.notFound(originalStartClass, !isClass(object));
            }
            foundField.setAccessible(true);
            return foundField;
        }

        /**
         * Checks for field proper modifier.
         *
         * @param object the object
         * @param field  the field
         * @return true, if successful
         */
        private static boolean hasFieldProperModifier(Object object, Field field) {
            return ((object instanceof Class<?> && Modifier.isStatic(field.getModifiers()))
                    || ((!(object instanceof Class<?>) && !Modifier.isStatic(field.getModifiers()))));
        }


        /**
         * Gets the field.
         *
         * @param fieldName the field name
         * @param where     the where
         * @return the field
         */
        private static Field getField(String fieldName, Class<?> where) {
            if (where == null) {
                throw new IllegalArgumentException("where cannot be null");
            }

            Field field = null;
            try {
                field = where.getDeclaredField(fieldName);
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new FieldNotFoundException("Field '" + fieldName + "' was not found in class " + where.getName()
                        + ".");
            }
            return field;
        }

        /**
         * Find field or throw exception.
         *
         * @param fieldType the field type
         * @param where     the where
         * @return the field
         */
        private static Field findFieldOrThrowException(Class<?> fieldType, Class<?> where) {
            if (fieldType == null || where == null) {
                throw new IllegalArgumentException("fieldType and where cannot be null");
            }
            Field field = null;
            for (Field currentField : where.getDeclaredFields()) {
                currentField.setAccessible(true);
                if (currentField.getType().equals(fieldType)) {
                    field = currentField;
                    break;
                }
            }
            if (field == null) {
                throw new FieldNotFoundException("Cannot find a field of type " + fieldType + "in where.");
            }
            return field;
        }

        /**
         * Assert object in get internal state is not null.
         *
         * @param object the object
         */
        private static void assertObjectInGetInternalStateIsNotNull(Object object) {
            if (object == null) {
                throw new IllegalArgumentException("The object containing the field cannot be null");
            }
        }

        /**
         * Gets the type.
         *
         * @param object the object
         * @return The type of the of an object.
         */
        public static Class<?> getType(Object object) {
            Class<?> type = null;
            if (isClass(object)) {
                type = (Class<?>) object;
            } else if (object != null) {
                type = object.getClass();
            }
            return type;
        }

        /**
         * Checks if is class.
         *
         * @param argument the argument
         * @return true, if is class
         */
        public static boolean isClass(Object argument) {
            return argument instanceof Class<?>;
        }

        /**
         * Sets the field.
         *
         * @param object     the object
         * @param value      the value
         * @param foundField the found field
         */
        private static void setField(Object object, Object value, Field foundField) {
            foundField.setAccessible(true);
            try {
                int fieldModifiersMask = foundField.getModifiers();
                removeFinalModifierIfPresent(foundField);
                foundField.set(object, value);
                restoreModifiersToFieldIfChanged(fieldModifiersMask, foundField);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Internal error: Failed to set field in method setInternalState.", e);
            }
        }

        private static void removeFinalModifierIfPresent(Field fieldToRemoveFinalFrom) throws IllegalAccessException {
            int fieldModifiersMask = fieldToRemoveFinalFrom.getModifiers();
            boolean isFinalModifierPresent = (fieldModifiersMask & Modifier.FINAL) == Modifier.FINAL;
            if (isFinalModifierPresent) {
                checkIfCanSetNewValue(fieldToRemoveFinalFrom);
                int fieldModifiersMaskWithoutFinal = fieldModifiersMask & ~Modifier.FINAL;
                sedModifiersToField(fieldToRemoveFinalFrom, fieldModifiersMaskWithoutFinal);
            }
        }

        private static void checkIfCanSetNewValue(Field fieldToSetNewValueTo) {
            int fieldModifiersMask = fieldToSetNewValueTo.getModifiers();
            boolean isFinalModifierPresent = (fieldModifiersMask & Modifier.FINAL) == Modifier.FINAL;
            boolean isStaticModifierPresent = (fieldModifiersMask & Modifier.STATIC) == Modifier.STATIC;

            if (isFinalModifierPresent && isStaticModifierPresent) {
                boolean fieldTypeIsPrimitive = fieldToSetNewValueTo.getType().isPrimitive();
                if (fieldTypeIsPrimitive) {
                    throw new IllegalArgumentException(
                            "You are trying to set a private static final primitive. " +
                                    "Try using an object like Integer instead of int!");
                }
                boolean fieldTypeIsString = fieldToSetNewValueTo.getType().equals(String.class);
                if (fieldTypeIsString) {
                    throw new IllegalArgumentException(
                            "You are trying to set a private static final String. Cannot set such fields!");
                }
            }
        }

        private static void restoreModifiersToFieldIfChanged(int initialFieldModifiersMask,
                                                             Field fieldToRestoreModifiersTo)
                throws IllegalAccessException {
            int newFieldModifiersMask = fieldToRestoreModifiersTo.getModifiers();
            if (initialFieldModifiersMask != newFieldModifiersMask) {
                sedModifiersToField(fieldToRestoreModifiersTo, initialFieldModifiersMask);
            }
        }

        private static void sedModifiersToField(Field fieldToRemoveFinalFrom, int fieldModifiersMaskWithoutFinal)
                throws IllegalAccessException {
            try {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                boolean accessibleBeforeSet = modifiersField.isAccessible();
                modifiersField.setAccessible(true);
                modifiersField.setInt(fieldToRemoveFinalFrom, fieldModifiersMaskWithoutFinal);
                modifiersField.setAccessible(accessibleBeforeSet);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(
                        "Internal error: Failed to find the \"modifiers\" field in method setInternalState.", e);
            }
        }

    }


    /**
     * A run-time exception that may be thrown to indicate that a field was not
     * found.
     */
    public static class FieldNotFoundException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 5420195402982130931L;

        /**
         * Constructs a new exception with the specified detail message. The cause
         * is not initialized, and may subsequently be initialized by a call to
         * {@link #initCause}.
         *
         * @param message the detail message. The detail message is saved for later
         *                retrieval by the {@link #getMessage()} method.
         */
        public FieldNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * A run-time exception that may be thrown to indicate that too many fields were
     * found.
     */
    public static class TooManyFieldsFoundException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1564231184610341053L;

        /**
         * Constructs a new exception with the specified detail message. The cause
         * is not initialized, and may subsequently be initialized by a call to
         * {@link #initCause}.
         *
         * @param message the detail message. The detail message is saved for later
         *                retrieval by the {@link #getMessage()} method.
         */
        public TooManyFieldsFoundException(String message) {
            super(message);
        }
    }

    public static abstract class FieldMatcherStrategy {

        /**
         * A field matcher that checks if a field matches a given criteria.
         *
         * @param field The field to check whether it matches the strategy or not.
         * @return {@code true} if this field matches the strategy,
         * {@code false} otherwise.
         */
        public abstract boolean matches(Field field);

        /**
         * Throws an {@link FieldNotFoundException} if the strategy criteria could
         * not be found.
         *
         * @param type            The type of the object that was not found.
         * @param isInstanceField {@code true} if the field that was looked after was an
         *                        instance field or {@code false} if it was a static field.
         */
        public abstract void notFound(Class<?> type, boolean isInstanceField) throws FieldNotFoundException;
    }

    public static class FieldNameMatcherStrategy extends FieldMatcherStrategy {

        private final String fieldName;

        public FieldNameMatcherStrategy(String fieldName) {
            if (fieldName == null || fieldName.isEmpty() || fieldName.startsWith(" ")) {
                throw new IllegalArgumentException("field name cannot be null.");
            }
            this.fieldName = fieldName;
        }

        @Override
        public boolean matches(Field field) {
            return fieldName.equals(field.getName());
        }

        @Override
        public void notFound(Class<?> type, boolean isInstanceField) throws FieldNotFoundException {
            throw new FieldNotFoundException(
                    String.format("No %s field named \"%s\" could be found in the class hierarchy of %s.",
                    isInstanceField ? "instance" : "static", fieldName, type.getName()));
        }

        @Override
        public String toString() {
            return "fieldName " + fieldName;
        }
    }

    public static class FieldTypeMatcherStrategy extends FieldMatcherStrategy {

        final Class<?> expectedFieldType;

        public FieldTypeMatcherStrategy(Class<?> fieldType) {
            if (fieldType == null) {
                throw new IllegalArgumentException("field type cannot be null.");
            }
            this.expectedFieldType = fieldType;
        }

        @Override
        public boolean matches(Field field) {
            return expectedFieldType.equals(field.getType());
        }

        @Override
        public void notFound(Class<?> type, boolean isInstanceField) throws FieldNotFoundException {
            throw new FieldNotFoundException(
                    String.format("No %s field of type \"%s\" could be found in the class hierarchy of %s.",
                    isInstanceField ? "instance" : "static", expectedFieldType.getName(), type.getName()));
        }

        @Override
        public String toString() {
            return "type " + expectedFieldType.getName();
        }
    }

    public static class AssignableToFieldTypeMatcherStrategy extends FieldTypeMatcherStrategy {

        public AssignableToFieldTypeMatcherStrategy(Class<?> fieldType) {
            super(fieldType);
        }

        @Override
        public boolean matches(Field field) {
            return expectedFieldType.isAssignableFrom(field.getType());
        }
    }

    public static class AssignableFromFieldTypeMatcherStrategy extends FieldTypeMatcherStrategy {

        private final Class<?> primitiveCounterpart;

        public AssignableFromFieldTypeMatcherStrategy(Class<?> fieldType) {
            super(fieldType);
            primitiveCounterpart = PrimitiveWrapper.getPrimitiveFromWrapperType(expectedFieldType);
        }

        @Override
        public boolean matches(Field field) {
            Class<?> actualFieldType = field.getType();
            return actualFieldType.isAssignableFrom(expectedFieldType)
                    || (primitiveCounterpart != null && actualFieldType.isAssignableFrom(primitiveCounterpart));
        }

        @Override
        public void notFound(Class<?> type, boolean isInstanceField) throws FieldNotFoundException {
            throw new FieldNotFoundException(
                    String.format("No %s field assignable from \"%s\" could be found in the class hierarchy of %s.",
                    isInstanceField ? "instance" : "static", expectedFieldType.getName(), type.getName()));
        }

        @Override
        public String toString() {
            return "type "
                    + (primitiveCounterpart == null ? expectedFieldType.getName() : primitiveCounterpart.getName());
        }
    }

    /**
     * The purpose of the Primitive Wrapper is to provide methods that deals with
     * translating wrapper types to its related primitive type.
     */
    public static class PrimitiveWrapper {
        private static final Map<Class<?>, Class<?>> primitiveWrapper = new HashMap<>();

        static {
            primitiveWrapper.put(Integer.class, int.class);
            primitiveWrapper.put(Long.class, long.class);
            primitiveWrapper.put(Float.class, float.class);
            primitiveWrapper.put(Double.class, double.class);
            primitiveWrapper.put(Boolean.class, boolean.class);
            primitiveWrapper.put(Byte.class, byte.class);
            primitiveWrapper.put(Short.class, short.class);
            primitiveWrapper.put(Character.class, char.class);
        }

        /**
         * Convert all wrapper types in {@code types} to their primitive
         * counter parts.
         *
         * @param types The array of types that should be converted.
         * @return A new array where all wrapped types have been converted to their
         * primitive counter part.
         */
        public static Class<?>[] toPrimitiveType(Class<?>[] types) {
            if (types == null) {
                throw new IllegalArgumentException("types cannot be null");
            }

            Class<?>[] convertedTypes = new Class<?>[types.length];
            for (int i = 0; i < types.length; i++) {
                final Class<?> originalType = types[i];
                Class<?> primitiveType = primitiveWrapper.get(originalType);
                if (primitiveType == null) {
                    convertedTypes[i] = originalType;
                } else {
                    convertedTypes[i] = primitiveType;
                }
            }
            return convertedTypes;
        }

        /**
         * Get the primitive counter part from a wrapped type. For example:
         * <p>
         * <p>
         * {@code getPrimitiveFromWrapperType(Integer.class)} will return
         * {@code int.class}.
         *
         * @param wrapperType The wrapper type to convert to its primitive counter part.
         * @return The primitive counter part or {@code null} if the class did
         * not have a primitive counter part.
         */
        public static Class<?> getPrimitiveFromWrapperType(Class<?> wrapperType) {
            return primitiveWrapper.get(wrapperType);
        }

        /**
         * Returns {@code true} if {@code type} has a primitive
         * counter-part. E.g. if {@code type} if {@code Integer} then this
         * method will return {@code true}.
         *
         * @param type The type to check whether or not it has a primitive
         *             counter-part.
         * @return {@code true} if this type has a primitive counter-part.
         */
        public static boolean hasPrimitiveCounterPart(Class<?> type) {
            return primitiveWrapper.containsKey(type);
        }
    }
}
//CHECKSTYLE:ON
