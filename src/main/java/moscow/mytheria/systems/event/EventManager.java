package moscow.mytheria.systems.event;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import moscow.mytheria.Mytheria;

public class EventManager {
   private final ConcurrentHashMap<Type, CopyOnWriteArrayList<EventListener<?>>> listenerMap = new ConcurrentHashMap<>();
   private final Map<Class<?>, Field[]> declaredFieldsCache = new HashMap<>();
   private final Comparator<EventListener<?>> priorityOrder = Comparator.comparingInt((EventListener<?> listener) -> listener.getPriority()).reversed();
   private final BiConsumer<List<EventListener<?>>, Comparator<EventListener<?>>> sortCallback = List::sort;
   private final Consumer<Throwable> errorHandler = Throwable::printStackTrace;

   public void subscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         CopyOnWriteArrayList<EventListener<?>> listeners = this.listenerMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
         if (!listeners.contains(listener)) {
            listeners.add(listener);
            this.sortCallback.accept(listeners, this.priorityOrder);
         }
      });
   }

   public void unsubscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         CopyOnWriteArrayList<EventListener<?>> listeners = this.listenerMap.get(type);
         if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
               this.listenerMap.remove(type);
            }
         }
      });
   }

   public <T extends Event> void triggerEvent(T event) {
      Class<?> eventType = event.getClass();
      List<EventListener<?>> listeners = this.listenerMap.get(eventType);
      if (listeners != null && !Mytheria.INSTANCE.isPanic()) {
         for (EventListener<?> listener : listeners) {
            try {
               ((EventListener<T>)listener).onEvent(event);
            } catch (Throwable throwable) {
               this.errorHandler.accept(throwable);
            }
         }
      }
   }

   private void modifyEventListenerState(Object subscriber, BiConsumer<Type, EventListener<?>> action) {
      for (Field field : this.getCachedDeclaredFields(subscriber.getClass())) {
         EventListener<?> eventListener;
         if (field.getType() != EventListener.class || (eventListener = this.getEventListener(subscriber, field)) == null) {
            continue;
         }

         Type eventType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
         action.accept(eventType, eventListener);
      }
   }

   private Field[] getCachedDeclaredFields(Class<?> clazz) {
      return this.declaredFieldsCache.computeIfAbsent(clazz, this::collectDeclaredFields);
   }

   private Field[] collectDeclaredFields(Class<?> clazz) {
      List<Field> fields = new ArrayList<>();
      Class<?> current = clazz;
      while (current != null && current != Object.class) {
         for (Field field : current.getDeclaredFields()) {
            fields.add(field);
         }
         current = current.getSuperclass();
      }

      return fields.toArray(new Field[0]);
   }

   private EventListener<?> getEventListener(Object subscriber, Field field) {
      boolean accessible = field.canAccess(subscriber);
      field.setAccessible(true);
      try {
         return (EventListener<?>)field.get(subscriber);
      } catch (IllegalAccessException exception) {
         this.errorHandler.accept(exception);
         return null;
      } finally {
         field.setAccessible(accessible);
      }
   }
}
