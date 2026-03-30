package com.example.slime.entities;

/**
 * Animation states for the Slime character.
 *
 * State machine (self-managed inside Slime.updateAnimation()):
 *
 *   IDLE ──────────────────────────────── menu screen static pose
 *
 *   On platform hit:
 *   FALLING ──► LANDING ──► LAUNCH ──► FALLING
 *               (60ms/f)   (100ms/f)   (static)
 *               fast flat  elongated   round
 *               one-shot   one-shot    held
 */
public enum SlimeState {
    /** Menu screen – static round frame, gentle visual bob. */
    IDLE,

    /** Just hit a platform – flat squish frames, very fast (60 ms/frame). */
    LANDING,

    /** Launching upward – elongated frames, medium speed (100 ms/frame), played once. */
    LAUNCH,

    /** In the air (rising or falling) – single round frame, held statically. */
    FALLING
}
