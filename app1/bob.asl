!check(slots).

/* Plans */

// Continue checking slots if no garbage is found
+!check(slots) : not garbage(alice)
    <- next(slot);
       !check(slots).

// Stop checking if garbage is found
+!check(slots) : garbage(alice)
    <- stopPlan.
