package edu.umass.cs.dispersible.models;

import edu.umass.cs.nio.interfaces.IntegerPacketType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Sarthak Nandi on 22/4/18.
 */
public enum DispersiblePacketType implements IntegerPacketType {

  NEW_APP(1000),
  EXECUTE(2000);

  private static final Map<Integer, DispersiblePacketType> INTEGER_TO_PACKET_TYPE;
  private final int integerPacketType;

  static {
    INTEGER_TO_PACKET_TYPE = Arrays.stream(DispersiblePacketType.values())
        .collect(Collectors.toMap(DispersiblePacketType::getInt, Function.identity()));

  }

  DispersiblePacketType(int integerPacketType) {
    this.integerPacketType = integerPacketType;
  }

  @Override
  public int getInt() {
    return integerPacketType;
  }

  public static Optional<DispersiblePacketType> getPacketType(int integerPacketType) {
    return Optional.ofNullable(INTEGER_TO_PACKET_TYPE.get(integerPacketType));
  }

  public static Set<IntegerPacketType> allDispersiblePacketTypes() {
    return new HashSet<>(INTEGER_TO_PACKET_TYPE.values());
  }
}
