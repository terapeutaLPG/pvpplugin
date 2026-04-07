package pl.jaruso99.pvp.model;

import org.bukkit.Location;

public class Arena {

    private final int id;
    private Location spawn1;
    private Location spawn2;
    private boolean occupied;

    public Arena(int id, Location spawn1, Location spawn2) {
        this.id = id;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.occupied = false;
    }

    public int getId() { return id; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
}
