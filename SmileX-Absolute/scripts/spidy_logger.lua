-- [[ SMILE-X: SPIDY LOGGER SYSTEM ]]
-- ระบบรายงานผลผ่าน Webhook โดยตรงไปยัง Discord

local HttpService = game:GetService("HttpService")
local WebhookURL = "https://discord.com/api/webhooks/1493160419571929129/Py-2cJ2ydyRZ1OwzkVZk8IRM6N8GCrS3qVR8Q99htWyE6Ya5GyPGjAGAHDTX_F8dZKmM"

local SpidyLogger = {}

-- ฟังก์ชันสำหรับส่ง Log แบบข้อความธรรมดา
function SpidyLogger:SendMessage(content)
    local data = {
        ["content"] = "💀 **[Smile-X Absolute]** " .. content
    }
    
    local success, err = pcall(function()
        HttpService:PostAsync(WebhookURL, HttpService:JSONEncode(data))
    end)
    
    if not success then
        warn("Spidy Logger Error: " .. err)
    end
end

-- ฟังก์ชันสำหรับส่ง Log แบบ Embed (สวยงามและดูเป็นระเบียบ)
function SpidyLogger:SendReport(title, detail, color)
    local data = {
        ["embeds"] = {{
            ["title"] = "🟢 " .. title,
            ["description"] = detail,
            ["color"] = color or 0x00FF80, -- สีเขียวมรกตเป็นค่าเริ่มต้น
            ["fields"] = {
                {
                    ["name"] = "Map ID",
                    ["value"] = tostring(game.PlaceId),
                    ["inline"] = true
                },
                {
                    ["name"] = "Player",
                    ["value"] = game.Players.LocalPlayer.Name,
                    ["inline"] = true
                }
            },
            ["footer"] = {
                ["text"] = "Smile-X Absolute Edition | Powering by Pipe"
            },
            ["timestamp"] = os.date("!%Y-%m-%dT%H:%M:%SZ")
        }}
    }

    pcall(function()
        HttpService:PostAsync(WebhookURL, HttpService:JSONEncode(data))
    end)
end

-- เมื่อโหลดสคริปต์สำเร็จ ให้รายงานตัวทันที
SpidyLogger:SendReport(
    "Smile-X Injected", 
    "ระบบเจาะ Identity 8 ทำงานแล้ว พร้อมรัน ServerScript อิสระ!", 
    0x00FF00
)

return SpidyLogger

