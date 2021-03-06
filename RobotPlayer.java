package pancakedogcorn;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static final Direction SOUTHEAST = new Direction ((float)Math.PI * -1/4);
    static final Direction NORTHWEST = SOUTHEAST.opposite();
    static Direction directionCurrent;
    static int soldierMode;  // 0 = hunting for prey, 1 = engaged in battle
    static Direction archonDirectionCurrent;
    static Direction gardenerDirectionCurrent;
    static Team enemy;
    static boolean escaping;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        directionCurrent = SOUTHEAST;
        soldierMode = 0;
        archonDirectionCurrent = NORTHWEST;
        gardenerDirectionCurrent = randomDirection();
        enemy = rc.getTeam().opponent();
        escaping = false;
        
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
        }
	}

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .3) {
                    rc.hireGardener(dir);
                }

               // call for help if sensing enemy HELP ME! overwrite battles.
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
                if (enemies.length > 0) {
                     System.out.println("new enemy location");
                     rc.broadcast(100, (int)enemies[0].location.x);
                     rc.broadcast(101, (int)enemies[0].location.y);
                     rc.broadcast(102, rc.getRoundNum() + 20); // expiration date
                }
                
                //should I be escaping?
                if (rc.readBroadcast(102) > 0) {
                     int x = rc.readBroadcast(100);
                     int y = rc.readBroadcast(101);
                     Direction d = rc.getLocation().directionTo(new MapLocation(x, y)).opposite();
                     d = d.rotateRightDegrees(90);
//                      if (!rc.canMove(d)) {
//                         d = d.rotateLeftDegrees(90);
//                      } else {
//                         while (!rc.canMove(d)) {
//                            d = d.rotateLeftDegrees(10);
//                         }
//                      }
                     archonDirectionCurrent = d;
                }
                
                tryMove(archonDirectionCurrent);
               
                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();
  
                                

                // Randomly attempt to build a soldier or lumberjack in this direction
                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < 1) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                }

               // call for help if sensing enemy HELP ME! overwrite battles.
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
                if (enemies.length > 0) {
                     System.out.println("new enemy location");
                     rc.broadcast(100, (int)enemies[0].location.x);
                     rc.broadcast(101, (int)enemies[0].location.y);
                     rc.broadcast(102, rc.getRoundNum() + 20); // expiration date
                }
                
//                 // back away slowly
//                 if (rc.getRoundNum() < rc.readBroadcast(102)) {
//                   System.out.println("Battle detected! Escaping to");
//                   gardenerDirectionCurrent = escapeDirection();
//                   System.out.println(gardenerDirectionCurrent);
//                 }
                
                //tryMove(gardenerDirectionCurrent);
                                
                // ring around archon
               MapLocation archonLocation = new MapLocation(rc.readBroadcast(0), rc.readBroadcast(1));
               if (rc.getLocation().distanceTo(archonLocation) > 11) {
                  tryMove(rc.getLocation().directionTo(archonLocation));
               } else if (rc.getLocation().distanceTo(archonLocation) < 9) {
                  tryMove(rc.getLocation().directionTo(archonLocation).opposite());
               }

 
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                     if (rc.getRoundNum() > rc.readBroadcast(102)) {
                        System.out.println("new enemy location");
                        rc.broadcast(100, (int)robots[0].location.x);
                        rc.broadcast(101, (int)robots[0].location.y);
                        rc.broadcast(102, rc.getRoundNum() + 20); // expiration date
                     }
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        Direction toEnemy = rc.getLocation().directionTo(robots[0].location);
                        rc.fireSingleShot(toEnemy);
                        soldierMode = 1;
                        directionCurrent = toEnemy.rotateRightDegrees(90); 

                     }
                } else { // if there are no robots in sight
                     soldierMode = 0;
                }
                    
               if(rc.getRoundNum() < rc.readBroadcast(102) && soldierMode == 0) {
                  int helpX = rc.readBroadcast(100);
                  int helpY = rc.readBroadcast(101);
                  directionCurrent = rc.getLocation().directionTo(new MapLocation(helpX, helpY));
               } else if (rc.getRoundNum() < 500) {
                  directionCurrent = randomDirection();
               }
               
               MapLocation archonLocation = new MapLocation(rc.readBroadcast(0), rc.readBroadcast(1));
               if (rc.getLocation().distanceTo(archonLocation) > 21) {
                  tryMove(rc.getLocation().directionTo(archonLocation));
               } else if (rc.getLocation().distanceTo(archonLocation) < 19) {
                  tryMove(rc.getLocation().directionTo(archonLocation).opposite());
               }
                 
//                if(rc.canMove(directionCurrent)) {
//                   tryMove(directionCurrent);
//                } else {
//                   directionCurrent = directionCurrent.opposite();
//                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // Move Randomly
                        tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
      
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
