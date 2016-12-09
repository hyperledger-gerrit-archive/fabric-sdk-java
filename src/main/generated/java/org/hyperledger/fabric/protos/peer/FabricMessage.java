// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: peer/fabric_message.proto

package org.hyperledger.fabric.protos.peer;

public final class FabricMessage {
  private FabricMessage() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface MessageOrBuilder extends
      // @@protoc_insertion_point(interface_extends:protos.Message)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * Type of this message.
     * </pre>
     *
     * <code>optional .protos.Message.Type type = 1;</code>
     */
    int getTypeValue();
    /**
     * <pre>
     * Type of this message.
     * </pre>
     *
     * <code>optional .protos.Message.Type type = 1;</code>
     */
    org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type getType();

    /**
     * <pre>
     * Version indicates message protocol version.
     * </pre>
     *
     * <code>optional int32 version = 2;</code>
     */
    int getVersion();

    /**
     * <pre>
     * The payload in this message. The way it should be unmarshaled
     * depends in the type field
     * </pre>
     *
     * <code>optional bytes payload = 3;</code>
     */
    com.google.protobuf.ByteString getPayload();
  }
  /**
   * <pre>
   * A Message encapsulates a payload of the indicated type in this message.
   * </pre>
   *
   * Protobuf type {@code protos.Message}
   */
  public  static final class Message extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:protos.Message)
      MessageOrBuilder {
    // Use Message.newBuilder() to construct.
    private Message(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private Message() {
      type_ = 0;
      version_ = 0;
      payload_ = com.google.protobuf.ByteString.EMPTY;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }
    private Message(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      int mutable_bitField0_ = 0;
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!input.skipField(tag)) {
                done = true;
              }
              break;
            }
            case 8: {
              int rawValue = input.readEnum();

              type_ = rawValue;
              break;
            }
            case 16: {

              version_ = input.readInt32();
              break;
            }
            case 26: {

              payload_ = input.readBytes();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.hyperledger.fabric.protos.peer.FabricMessage.internal_static_protos_Message_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.hyperledger.fabric.protos.peer.FabricMessage.internal_static_protos_Message_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.hyperledger.fabric.protos.peer.FabricMessage.Message.class, org.hyperledger.fabric.protos.peer.FabricMessage.Message.Builder.class);
    }

    /**
     * Protobuf enum {@code protos.Message.Type}
     */
    public enum Type
        implements com.google.protobuf.ProtocolMessageEnum {
      /**
       * <pre>
       * Undefined exists to prevent invalid message construction.
       * </pre>
       *
       * <code>UNDEFINED = 0;</code>
       */
      UNDEFINED(0),
      /**
       * <pre>
       * Handshake messages.
       * </pre>
       *
       * <code>DISCOVERY = 1;</code>
       */
      DISCOVERY(1),
      /**
       * <pre>
       * Sent to catch up with existing peers.
       * </pre>
       *
       * <code>SYNC = 2;</code>
       */
      SYNC(2),
      UNRECOGNIZED(-1),
      ;

      /**
       * <pre>
       * Undefined exists to prevent invalid message construction.
       * </pre>
       *
       * <code>UNDEFINED = 0;</code>
       */
      public static final int UNDEFINED_VALUE = 0;
      /**
       * <pre>
       * Handshake messages.
       * </pre>
       *
       * <code>DISCOVERY = 1;</code>
       */
      public static final int DISCOVERY_VALUE = 1;
      /**
       * <pre>
       * Sent to catch up with existing peers.
       * </pre>
       *
       * <code>SYNC = 2;</code>
       */
      public static final int SYNC_VALUE = 2;


      public final int getNumber() {
        if (this == UNRECOGNIZED) {
          throw new java.lang.IllegalArgumentException(
              "Can't get the number of an unknown enum value.");
        }
        return value;
      }

      /**
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @java.lang.Deprecated
      public static Type valueOf(int value) {
        return forNumber(value);
      }

      public static Type forNumber(int value) {
        switch (value) {
          case 0: return UNDEFINED;
          case 1: return DISCOVERY;
          case 2: return SYNC;
          default: return null;
        }
      }

      public static com.google.protobuf.Internal.EnumLiteMap<Type>
          internalGetValueMap() {
        return internalValueMap;
      }
      private static final com.google.protobuf.Internal.EnumLiteMap<
          Type> internalValueMap =
            new com.google.protobuf.Internal.EnumLiteMap<Type>() {
              public Type findValueByNumber(int number) {
                return Type.forNumber(number);
              }
            };

      public final com.google.protobuf.Descriptors.EnumValueDescriptor
          getValueDescriptor() {
        return getDescriptor().getValues().get(ordinal());
      }
      public final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptorForType() {
        return getDescriptor();
      }
      public static final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptor() {
        return org.hyperledger.fabric.protos.peer.FabricMessage.Message.getDescriptor().getEnumTypes().get(0);
      }

      private static final Type[] VALUES = values();

      public static Type valueOf(
          com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
          throw new java.lang.IllegalArgumentException(
            "EnumValueDescriptor is not for this type.");
        }
        if (desc.getIndex() == -1) {
          return UNRECOGNIZED;
        }
        return VALUES[desc.getIndex()];
      }

      private final int value;

      private Type(int value) {
        this.value = value;
      }

      // @@protoc_insertion_point(enum_scope:protos.Message.Type)
    }

    public static final int TYPE_FIELD_NUMBER = 1;
    private int type_;
    /**
     * <pre>
     * Type of this message.
     * </pre>
     *
     * <code>optional .protos.Message.Type type = 1;</code>
     */
    public int getTypeValue() {
      return type_;
    }
    /**
     * <pre>
     * Type of this message.
     * </pre>
     *
     * <code>optional .protos.Message.Type type = 1;</code>
     */
    public org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type getType() {
      org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type result = org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type.valueOf(type_);
      return result == null ? org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type.UNRECOGNIZED : result;
    }

    public static final int VERSION_FIELD_NUMBER = 2;
    private int version_;
    /**
     * <pre>
     * Version indicates message protocol version.
     * </pre>
     *
     * <code>optional int32 version = 2;</code>
     */
    public int getVersion() {
      return version_;
    }

    public static final int PAYLOAD_FIELD_NUMBER = 3;
    private com.google.protobuf.ByteString payload_;
    /**
     * <pre>
     * The payload in this message. The way it should be unmarshaled
     * depends in the type field
     * </pre>
     *
     * <code>optional bytes payload = 3;</code>
     */
    public com.google.protobuf.ByteString getPayload() {
      return payload_;
    }

    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (type_ != org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type.UNDEFINED.getNumber()) {
        output.writeEnum(1, type_);
      }
      if (version_ != 0) {
        output.writeInt32(2, version_);
      }
      if (!payload_.isEmpty()) {
        output.writeBytes(3, payload_);
      }
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (type_ != org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type.UNDEFINED.getNumber()) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(1, type_);
      }
      if (version_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(2, version_);
      }
      if (!payload_.isEmpty()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(3, payload_);
      }
      memoizedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof org.hyperledger.fabric.protos.peer.FabricMessage.Message)) {
        return super.equals(obj);
      }
      org.hyperledger.fabric.protos.peer.FabricMessage.Message other = (org.hyperledger.fabric.protos.peer.FabricMessage.Message) obj;

      boolean result = true;
      result = result && type_ == other.type_;
      result = result && (getVersion()
          == other.getVersion());
      result = result && getPayload()
          .equals(other.getPayload());
      return result;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptorForType().hashCode();
      hash = (37 * hash) + TYPE_FIELD_NUMBER;
      hash = (53 * hash) + type_;
      hash = (37 * hash) + VERSION_FIELD_NUMBER;
      hash = (53 * hash) + getVersion();
      hash = (37 * hash) + PAYLOAD_FIELD_NUMBER;
      hash = (53 * hash) + getPayload().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(org.hyperledger.fabric.protos.peer.FabricMessage.Message prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * <pre>
     * A Message encapsulates a payload of the indicated type in this message.
     * </pre>
     *
     * Protobuf type {@code protos.Message}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:protos.Message)
        org.hyperledger.fabric.protos.peer.FabricMessage.MessageOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return org.hyperledger.fabric.protos.peer.FabricMessage.internal_static_protos_Message_descriptor;
      }

      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return org.hyperledger.fabric.protos.peer.FabricMessage.internal_static_protos_Message_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                org.hyperledger.fabric.protos.peer.FabricMessage.Message.class, org.hyperledger.fabric.protos.peer.FabricMessage.Message.Builder.class);
      }

      // Construct using org.hyperledger.fabric.protos.peer.FabricMessage.Message.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      public Builder clear() {
        super.clear();
        type_ = 0;

        version_ = 0;

        payload_ = com.google.protobuf.ByteString.EMPTY;

        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return org.hyperledger.fabric.protos.peer.FabricMessage.internal_static_protos_Message_descriptor;
      }

      public org.hyperledger.fabric.protos.peer.FabricMessage.Message getDefaultInstanceForType() {
        return org.hyperledger.fabric.protos.peer.FabricMessage.Message.getDefaultInstance();
      }

      public org.hyperledger.fabric.protos.peer.FabricMessage.Message build() {
        org.hyperledger.fabric.protos.peer.FabricMessage.Message result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public org.hyperledger.fabric.protos.peer.FabricMessage.Message buildPartial() {
        org.hyperledger.fabric.protos.peer.FabricMessage.Message result = new org.hyperledger.fabric.protos.peer.FabricMessage.Message(this);
        result.type_ = type_;
        result.version_ = version_;
        result.payload_ = payload_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof org.hyperledger.fabric.protos.peer.FabricMessage.Message) {
          return mergeFrom((org.hyperledger.fabric.protos.peer.FabricMessage.Message)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(org.hyperledger.fabric.protos.peer.FabricMessage.Message other) {
        if (other == org.hyperledger.fabric.protos.peer.FabricMessage.Message.getDefaultInstance()) return this;
        if (other.type_ != 0) {
          setTypeValue(other.getTypeValue());
        }
        if (other.getVersion() != 0) {
          setVersion(other.getVersion());
        }
        if (other.getPayload() != com.google.protobuf.ByteString.EMPTY) {
          setPayload(other.getPayload());
        }
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        org.hyperledger.fabric.protos.peer.FabricMessage.Message parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (org.hyperledger.fabric.protos.peer.FabricMessage.Message) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int type_ = 0;
      /**
       * <pre>
       * Type of this message.
       * </pre>
       *
       * <code>optional .protos.Message.Type type = 1;</code>
       */
      public int getTypeValue() {
        return type_;
      }
      /**
       * <pre>
       * Type of this message.
       * </pre>
       *
       * <code>optional .protos.Message.Type type = 1;</code>
       */
      public Builder setTypeValue(int value) {
        type_ = value;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * Type of this message.
       * </pre>
       *
       * <code>optional .protos.Message.Type type = 1;</code>
       */
      public org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type getType() {
        org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type result = org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type.valueOf(type_);
        return result == null ? org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type.UNRECOGNIZED : result;
      }
      /**
       * <pre>
       * Type of this message.
       * </pre>
       *
       * <code>optional .protos.Message.Type type = 1;</code>
       */
      public Builder setType(org.hyperledger.fabric.protos.peer.FabricMessage.Message.Type value) {
        if (value == null) {
          throw new NullPointerException();
        }
        
        type_ = value.getNumber();
        onChanged();
        return this;
      }
      /**
       * <pre>
       * Type of this message.
       * </pre>
       *
       * <code>optional .protos.Message.Type type = 1;</code>
       */
      public Builder clearType() {
        
        type_ = 0;
        onChanged();
        return this;
      }

      private int version_ ;
      /**
       * <pre>
       * Version indicates message protocol version.
       * </pre>
       *
       * <code>optional int32 version = 2;</code>
       */
      public int getVersion() {
        return version_;
      }
      /**
       * <pre>
       * Version indicates message protocol version.
       * </pre>
       *
       * <code>optional int32 version = 2;</code>
       */
      public Builder setVersion(int value) {
        
        version_ = value;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * Version indicates message protocol version.
       * </pre>
       *
       * <code>optional int32 version = 2;</code>
       */
      public Builder clearVersion() {
        
        version_ = 0;
        onChanged();
        return this;
      }

      private com.google.protobuf.ByteString payload_ = com.google.protobuf.ByteString.EMPTY;
      /**
       * <pre>
       * The payload in this message. The way it should be unmarshaled
       * depends in the type field
       * </pre>
       *
       * <code>optional bytes payload = 3;</code>
       */
      public com.google.protobuf.ByteString getPayload() {
        return payload_;
      }
      /**
       * <pre>
       * The payload in this message. The way it should be unmarshaled
       * depends in the type field
       * </pre>
       *
       * <code>optional bytes payload = 3;</code>
       */
      public Builder setPayload(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        payload_ = value;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * The payload in this message. The way it should be unmarshaled
       * depends in the type field
       * </pre>
       *
       * <code>optional bytes payload = 3;</code>
       */
      public Builder clearPayload() {
        
        payload_ = getDefaultInstance().getPayload();
        onChanged();
        return this;
      }
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }

      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }


      // @@protoc_insertion_point(builder_scope:protos.Message)
    }

    // @@protoc_insertion_point(class_scope:protos.Message)
    private static final org.hyperledger.fabric.protos.peer.FabricMessage.Message DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new org.hyperledger.fabric.protos.peer.FabricMessage.Message();
    }

    public static org.hyperledger.fabric.protos.peer.FabricMessage.Message getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Message>
        PARSER = new com.google.protobuf.AbstractParser<Message>() {
      public Message parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
          return new Message(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<Message> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Message> getParserForType() {
      return PARSER;
    }

    public org.hyperledger.fabric.protos.peer.FabricMessage.Message getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_protos_Message_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_protos_Message_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\031peer/fabric_message.proto\022\006protos\"\177\n\007M" +
      "essage\022\"\n\004type\030\001 \001(\0162\024.protos.Message.Ty" +
      "pe\022\017\n\007version\030\002 \001(\005\022\017\n\007payload\030\003 \001(\014\".\n\004" +
      "Type\022\r\n\tUNDEFINED\020\000\022\r\n\tDISCOVERY\020\001\022\010\n\004SY" +
      "NC\020\002BO\n\"org.hyperledger.fabric.protos.pe" +
      "erZ)github.com/hyperledger/fabric/protos" +
      "/peerb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_protos_Message_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_protos_Message_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_protos_Message_descriptor,
        new java.lang.String[] { "Type", "Version", "Payload", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
