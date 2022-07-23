package ru.vichukano.bozar.http.actor;

import org.slf4j.Logger;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;

public abstract class AbstractActor<T> extends AbstractBehavior<T> {

  public AbstractActor(ActorContext<T> context) {
    super(context);
  }

  public Logger log() {
    return getContext().getLog();
  }

}
