(ns fress.reader
  (:require-macros [fress.macros :refer [<< >>>]])
  (:require [fress.impl.raw-input :as rawIn]
            [fress.codes :as codes]
            [fress.ranges :as ranges]
            [goog.string :as gstring])
  (:import [goog.math Long]))

(defn log [& args] (.apply js/console.log js/console (into-array args)))

(defn ^int internalReadInt32 [this])
(defn ^bytes internalReadString [this count])
(defn ^bytes internalReadStringBuffer [this])
(defn ^string internalReadChunkedString [this count])
(defn ^bytes internalReadBytes [this length])
(defn ^bytes internalReadChunkedBytes [this length])
(defn ^number internalReadDouble [rdr code])

(defprotocol IFressianReader
  (read [this code])
  (readNextCode [this])
  (readBoolean [this])
  (readInt [this])
  (readDouble [this])
  (readFloat [this])
  (readInt32 [this])
  (readObject [this])
  (readCount [this])
  (readObjects [this])
  (readClosedList [this])
  (readOpenList [this])
  (readAndCacheObject [this cache])
  (validateFooter [this]
                  [this calculatedLength magicFromStream])
  (handleStruct [this ^string tag fields])
  (getHandler [this ^string tag])
  (getPriorityCache [this])
  (getStructCache [this])
  (resetCaches [this]))

(defn ^number internalReadInt
  ([rdr](internalReadInt rdr (readNextCode rdr) ))
  ([rdr code]
   (cond
     (== code 0xFF) -1

     (<= 0x00 code 0x3F)
     (bit-and code 0xFF)

     (<= 0x40 code 0x5F)
     (bit-or (<< (- code codes/INT_PACKED_2_ZERO) 8) (rawIn/readRawInt8 (.-raw-in rdr)))

     (<= 0x60 code 0x6F)
     (bit-or (<< (- code codes/INT_PACKED_3_ZERO) 16) (rawIn/readRawInt16 (.-raw-in rdr)))

     (<= 0x70 code 0x73)
     (bit-or (<< (- code codes/INT_PACKED_4_ZERO) 24) (rawIn/readRawInt24 (.-raw-in rdr)))

     (<= 0x74 code 0x77)
     (let [packing (Long.fromNumber (- code codes/INT_PACKED_5_ZERO))
           i32 (Long.fromNumber (rawIn/readRawInt32 (.-raw-in rdr)))]
       (.toNumber (.or (.shiftLeft packing 32) i32)))

     (<= 0x78 code 0x7B)
     (let [packing (Long.fromNumber (- code codes/INT_PACKED_6_ZERO))
           i40 (Long.fromNumber (rawIn/readRawInt40 (.-raw-in rdr)))]
       (.toNumber (.or (.shiftLeft packing 40) i40)))

     (<= 0x7C code 0x7F)
     (let [packing (Long.fromNumber (- code codes/INT_PACKED_7_ZERO))
           i48 (Long.fromNumber (rawIn/readRawInt48 (.-raw-in rdr)))]
       (.toNumber (.or (.shiftLeft packing 48) i48)))

     (== code codes/INT)
     (rawIn/readRawInt64 (.-raw-in rdr))

     :default
     (let [o (read rdr code)]
       (if (number? o) o
         (throw (js/Error. (str "unexpected int64" code o))))))))

(defn internalRead [rdr ^number code]
  (let []
    (cond
      (or (== code 0xFF)
          (<= 0x00 code 0x7F)
          (== code codes/INT))
      (internalReadInt rdr code)

      (== code codes/PUT_PRIORITY_CACHE)
      (readAndCacheObject rdr (getPriorityCache rdr))

      (== code codes/GET_PRIORITY_CACHE)
      (lookupCache rdr (getPriorityCache rdr) (readInt32 rdr))

      (or
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 0))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 1))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 2))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 3))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 4))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 5))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 6))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 7))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 8))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 9))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 10))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 11))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 12))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 13))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 14))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 15))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 16))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 17))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 18))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 19))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 20))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 21))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 22))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 23))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 24))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 25))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 26))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 27))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 28))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 29))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 30))
       (== code (+ codes/PRIORITY_CACHE_PACKED_START 31)))
      (lookupCache rdr (getPriorityCache rdr) (- code codes/PRIORITY_CACHE_PACKED_START))

      (or
       (== code (+ codes/STRUCT_CACHE_PACKED_START 0))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 1))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 2))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 3))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 4))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 5))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 6))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 7))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 8))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 9))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 10))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 11))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 12))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 13))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 14))
       (== code (+ codes/STRUCT_CACHE_PACKED_START 15)))
      (let [struct-type (lookupCache rdr (getStructCache rdr) (- code codes/STRUCT_CACHE_PACKED_START))]
        (handleStruct rdr (.-tag struct-type) (.-fields struct-type)))

      (== code codes/MAP)
      (handleStruct rdr "map" 1)

      (== code codes/SET)
      (handleStruct rdr "set" 1)

      (== code codes/UUID)
      (handleStruct rdr "uuid" 2)

      (== code codes/REGEX)
      (handleStruct rdr "regex" 1)

      (== code codes/URI)
      (handleStruct rdr "uri" 1)

      (== code codes/BIGINT)
      (handleStruct rdr "bigint" 1)

      (== code codes/BIGDEC)
      (handleStruct rdr "bigdec" 2)

      (== code codes/INST)
      (handleStruct rdr "inst" 1)

      (== code codes/SYM)
      (handleStruct rdr "sym" 2)

      (== code codes/KEY)
      (handleStruct rdr "key" 2)

      (== code codes/INT_ARRAY)
      (handleStruct rdr "int[]" 2)

      (== code codes/LONG_ARRAY)
      (handleStruct rdr "long[]" 2)

      (== code codes/FLOAT_ARRAY)
      (handleStruct rdr "float[]" 2)

      (== code codes/DOUBLE_ARRAY)
      (handleStruct rdr "double[]" 2)

      (== code codes/BOOLEAN_ARRAY)
      (handleStruct rdr "boolean[]" 2)

      (== code codes/OBJECT_ARRAY)
      (handleStruct rdr "Object[]" 2)

      (or
       (== code (+ codes/BYTES_PACKED_LENGTH_START 0))
       (== code (+ codes/BYTES_PACKED_LENGTH_START 1))
       (== code (+ codes/BYTES_PACKED_LENGTH_START 2))
       (== code (+ codes/BYTES_PACKED_LENGTH_START 3))
       (== code (+ codes/BYTES_PACKED_LENGTH_START 4))
       (== code (+ codes/BYTES_PACKED_LENGTH_START 5))
       (== code (+ codes/BYTES_PACKED_LENGTH_START 6))
       (== code (+ codes/BYTES_PACKED_LENGTH_START 7)))
      (internalReadBytes rdr (- code codes/BYTES_PACKED_LENGTH_START))

      (== code codes/BYTES)
      (internalReadBytes rdr (readCount rdr))

      (== code codes/BYTES_CHUNK)
      (internalReadChunkedBytes rdr (readCount rdr))

      (or
       (== code (+ codes/STRING_PACKED_LENGTH_START 0))
       (== code (+ codes/STRING_PACKED_LENGTH_START 1))
       (== code (+ codes/STRING_PACKED_LENGTH_START 2))
       (== code (+ codes/STRING_PACKED_LENGTH_START 3))
       (== code (+ codes/STRING_PACKED_LENGTH_START 4))
       (== code (+ codes/STRING_PACKED_LENGTH_START 5))
       (== code (+ codes/STRING_PACKED_LENGTH_START 6))
       (== code (+ codes/STRING_PACKED_LENGTH_START 7)))
      (internalReadString rdr (- code codes/STRING_PACKED_LENGTH_START)) ;=> string

      (== code codes/STRING)
      (internalReadString rdr (readCount rdr)) ;=> string

      (== code codes/STRING_CHUNK)
      (internalReadChunkedString rdr (readCount rdr)) ;=> string

      (or
       (== code (+ codes/LIST_PACKED_LENGTH_START 0))
       (== code (+ codes/LIST_PACKED_LENGTH_START 1))
       (== code (+ codes/LIST_PACKED_LENGTH_START 2))
       (== code (+ codes/LIST_PACKED_LENGTH_START 3))
       (== code (+ codes/LIST_PACKED_LENGTH_START 4))
       (== code (+ codes/LIST_PACKED_LENGTH_START 5))
       (== code (+ codes/LIST_PACKED_LENGTH_START 6))
       (== code (+ codes/LIST_PACKED_LENGTH_START 7)))
      (internalReadList rdr (- code codes/LIST_PACKED_LENGTH_START))

      (== code codes/LIST)
      (internalReadList rdr (readCount rdr))

      (== code codes/BEGIN_CLOSED_LIST)
      ; result = ((ConvertList) getHandler("list")).convertList(readClosedList());
      (let [handler (getHandler rdr "list")]
        (handler (readClosedList rdr)))

      (== code codes/BEGIN_OPEN_LIST)
      (let [handler (getHandler rdr "list")]
        ; result = ((ConvertList) getHandler("list")).convertList(readOpenList());
        (handler (readOpenList rdr)))

      (== code codes/TRUE)
      true

      (== code codes/FALSE)
      false

      (== code codes/NULL)
      nil

      (or
       (== code codes/DOUBLE)
       (== code codes/DOUBLE_0)
       (== code codes/DOUBLE_1))
      (let [handler (getHandler rdr "double")] ;=================>>>>>>>>>>>>>>>>>>>
        (handler (internalReadDouble rdr code)))

      (== code codes/FLOAT)
      (let [handler (getHandler rdr "float")] ;>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        (handler (readRawFloat (.-raw-in rdr))))

      (== code codes/FOOTER)
      (let [])


      )))

(defrecord FressianReader [in raw-in lookup]
  IFressianReader
  (readNextCode [this] (rawIn/readRawByte raw-in))
  (readInt [this] (internalReadInt this))
  (read [this code] (internalRead this code))
  (readFloat [this]
    (let [code (readNextCode this)]
      (if (== code codes/FLOAT)
        (rawIn/readRawFloat raw-in)
        (let [o (read this code)]
          (if (number? o)
            o
            (throw (js/Error. (str "Expected float " code o)))))))))


(def default-read-handlers
  {})

(defn reader
  ([in] (reader in nil))
  ([in user-handlers]
   (let [handlers (merge default-read-handlers user-handlers)
         raw-in (rawIn/raw-input in)]
     (FressianReader. in raw-in handlers))))