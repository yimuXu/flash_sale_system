-- keys[1] = stock
-- keys[2] = users
-- ARGV[1] = userId
-- return 1 success / -1 out_of_stock / -2 duplicate_order / -3 product_unpreheated

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2
end

local stock = tonumber(redis.call('GET',KEYS[1]))
if stock == nil then
    return -3
end
if stock <= 0 then
    return -1
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1