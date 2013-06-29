package com.incredibleMachines.powergarden;

public class Monkey<K, V> {

    public K key;
    public V value;

    public Monkey(K key, V value) {
	this.key = key;
	this.value = value;
    }

    public K getKey()	{ return key; }
    public V getValue() { return value; }
}
