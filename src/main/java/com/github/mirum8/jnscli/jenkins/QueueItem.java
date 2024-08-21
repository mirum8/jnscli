package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QueueItem(
    long id,
    boolean blocked,
    boolean buildable,
    boolean stuck,
    long inQueueSince,
    String why,
    String url,
    Task task,
    @JsonProperty("_class")
    String aClass
) {
    public QueueItemType type() {
        return QueueItemType.getForClass(aClass);
    }

    public record Task(
        String name,
        String url,
        String color
    ) {
    }

    public enum QueueItemType {
        BLOCKED_ITEM("hudson.model.Queue$BlockedItem"),
        BUILDABLE_ITEM("hudson.model.Queue$BuildableItem"),
        LEFT_ITEM("hudson.model.Queue$LeftItem"),
        WAITING_ITEM("hudson.model.Queue$WaitingItem"),
        FLYWEIGHT_TASK("hudson.model.Queue$FlyweightTask"),
        UNKNOWN("");

        private final String aClass;

        QueueItemType(String aClass) {
            this.aClass = aClass;
        }

        static QueueItemType getForClass(String aClass) {
            for (QueueItemType type : values()) {
                if (type.aClass.equals(aClass)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }
}
