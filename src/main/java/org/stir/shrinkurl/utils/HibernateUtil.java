package org.stir.shrinkurl.utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.entity.Subscription;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Create the SessionFactory from hibernate.properties
            // If you were using hibernate.cfg.xml, it would be new Configuration().configure();
            return new Configuration().configure("hibernate.properties")
                                      .addAnnotatedClass(User.class)
                                      .addAnnotatedClass(Subscription.class)
                                      .buildSessionFactory();
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (getSessionFactory() != null) {
            getSessionFactory().close();
        }
    }
}
