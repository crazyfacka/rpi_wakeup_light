package com.hairysoft.message;

import com.hairysoft.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class serves as a base for all the exchanged messages between the devices and should not be instantiated itself, but rather extended
 */
public abstract class BaseMessage {

    private final static String TAG = "BaseMessage";
    private final static List<Class<?>> registeredClasses = new ArrayList<>();

    /**
     * Method to hold all the classes that extend this one.
     * Should be called as soon as possible upon the start of the application.
     *
     * @param clazz Class that is extending this class
     */
    public static void registerClass(Class<?>... clazz) {
        registeredClasses.addAll(Arrays.asList(clazz));
    }

    public BaseMessage() { }

    /**
     * Returns the type of the class that extended this one
     *
     * @return String Name of the class that extended the BaseMessage class
     */
    private String type() {
        return this.getClass().getSimpleName().toLowerCase();
    }

    /**
     * Returns a JSON String representing the data that the current class is holding
     *
     * @return String JSON String representing all the stored data
     * @throws JSONException
     */
    public String getJSON() throws JSONException {
        String type = type();

        JSONObject json = new JSONObject();
        json.put("type", type);

        /* Use reflection to iterate through all the fields of the extended class, retrieving their names and their values
         * creating key/value pairs in the JSON message. This was to ease the process of creating new messages within the application.
         *
         * If you need to create a message with the 'type' key set as 'shenanigans' with a key called 'stuff' and other 'moreStuff', you do something of sorts:
         *
         * public class Shenanigans extends BaseMessage {
         *      public int stuff;
         *      public String moreStuff;
         *      public Shenanigans() { }
         * }
         *
         * And at the start of the application register it:
         *
         * BaseMessage.registerClass(Shenanigans.class);
         */

        Field[] fields = new Field[0];
        for(Class<?> clazz : registeredClasses) {
            if(clazz.getSimpleName().toLowerCase().equalsIgnoreCase(type)) {
                fields = clazz.getFields();
                break;
            }
        }

        for(Field field : fields) {
            try {
                json.put(field.getName(), field.get(this));
            } catch(Exception ex) {
                Log.e(TAG, "Reflection problems", ex);
            }
        }

        return json.toString();
    }

}
