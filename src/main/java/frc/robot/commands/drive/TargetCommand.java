// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.drive;

import java.util.ArrayList;
import java.util.function.Supplier;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Constants.FieldConstants;
import frc.robot.Constants.PathConstants;
import frc.robot.subsystems.DriveBase;

/**
 * Drives a {@link DriveBase} through predetermined waypoints to a target to score.
 */
public class TargetCommand extends DriveCommand {
  private Pose2d targetPose;
  private final Supplier<Pose2d> targetSupplier;

  /**
   * Drives a {@link DriveBase} through a trajectory of waypoints to reach a scoring target.
   */
  public TargetCommand(DriveBase driveBase, Supplier<Pose2d> targetSupplier) {
    super(driveBase);
    this.targetSupplier = targetSupplier;
  }

  @Override
  public void initialize() {
    this.targetPose = targetSupplier.get();
    /* Finding the waypoint closest to the target. */
    int finalWaypointIndex = 0;
    for (int i = 0; i < FieldConstants.TRAVELING_WAYPOINTS.length; i++) {
      if (Math.abs(targetPose.getY() - FieldConstants.TRAVELING_WAYPOINTS[i].getY()) < Math
          .abs(targetPose.getY() - FieldConstants.TRAVELING_WAYPOINTS[finalWaypointIndex].getY())) {
        finalWaypointIndex = i;
      }
    }
    /*
     * Finding the starting waypoint (closest to robot) and generating a path to the final waypoint. Logic is slightly
     * different depending on direction the robot is traveling.
     */
    ArrayList<Pose2d> waypoints = new ArrayList<>();
    if (driveBase.getDistanceToPose(targetPose).getY() < 0) {
      int index = 0;
      while (driveBase.getDistanceToPose(FieldConstants.TRAVELING_WAYPOINTS[index]).getY() > 0) {
        if (index >= FieldConstants.TRAVELING_WAYPOINTS.length - 1) {
          index = FieldConstants.TRAVELING_WAYPOINTS.length - 1;
          break;
        }
        index++;
      }
      if (DriverStation.getAlliance() == Alliance.Blue) {
        index = MathUtil.clamp(index - 1, 0, FieldConstants.TRAVELING_WAYPOINTS.length - 1);
        for (int i = index; i >= finalWaypointIndex; i--) {
          waypoints.add(driveBase.flipWaypointBasedOnAlliance(new Pose2d(FieldConstants.TRAVELING_WAYPOINTS[i].getX(),
              FieldConstants.TRAVELING_WAYPOINTS[i].getY(), Rotation2d.fromDegrees(-90)), false));
        }
      } else {
        index = MathUtil.clamp(index + 1, 0, FieldConstants.TRAVELING_WAYPOINTS.length - 1);
        for (int i = index; i < finalWaypointIndex; i++) {
          waypoints.add(driveBase.flipWaypointBasedOnAlliance(new Pose2d(FieldConstants.TRAVELING_WAYPOINTS[i].getX(),
              FieldConstants.TRAVELING_WAYPOINTS[i].getY(), Rotation2d.fromDegrees(90)), false));
        }
      }
    } else {
      int index = FieldConstants.TRAVELING_WAYPOINTS.length - 1;
      while (driveBase.getDistanceToPose(FieldConstants.TRAVELING_WAYPOINTS[index]).getY() < 0) {
        if (index == 0) {
          index = 0;
          break;
        }
        index--;
      }
      if (DriverStation.getAlliance() == Alliance.Blue) {
        index = MathUtil.clamp(index + 1, 0, FieldConstants.TRAVELING_WAYPOINTS.length - 1);
        for (int i = index; i < finalWaypointIndex; i++) {
          waypoints.add(driveBase.flipWaypointBasedOnAlliance(new Pose2d(FieldConstants.TRAVELING_WAYPOINTS[i].getX(),
              FieldConstants.TRAVELING_WAYPOINTS[i].getY(), Rotation2d.fromDegrees(90)), false));
        }
      } else {
        index = MathUtil.clamp(index - 1, 0, FieldConstants.TRAVELING_WAYPOINTS.length - 1);
        for (int i = index; i >= finalWaypointIndex; i--) {
          waypoints.add(driveBase.flipWaypointBasedOnAlliance(new Pose2d(FieldConstants.TRAVELING_WAYPOINTS[i].getX(),
              FieldConstants.TRAVELING_WAYPOINTS[i].getY(), Rotation2d.fromDegrees(-90)), false));
        }
      }
    }

    if (waypoints.size() <= 0) {
      return;
    }
    CommandScheduler.getInstance().schedule(new PathFollowCommand(driveBase, driveBase.generatePath(waypoints))
        .andThen(new TurnToAngleCommand(driveBase, targetPose.getRotation(), PathConstants.TURN_ANGLE_DEADBAND))
        .andThen(new PathFollowCommand(driveBase, () -> driveBase.generatePath(targetPose)))
        .andThen(new TurnToAngleCommand(driveBase, targetPose.getRotation(), PathConstants.TURN_ANGLE_DEADBAND)));
  }


  @Override
  public boolean isFinished() {
    return true;
  }
}
