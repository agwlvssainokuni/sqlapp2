/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react'
import { useTranslation } from 'react-i18next'

const LanguageSwitcher: React.FC = () => {
  const { i18n } = useTranslation()

  const handleLanguageChange = (language: string) => {
    i18n.changeLanguage(language)
  }

  const currentLanguage = i18n.language || 'en'

  return (
    <div className="language-switcher">
      <select 
        value={currentLanguage} 
        onChange={(e) => handleLanguageChange(e.target.value)}
        className="language-select"
      >
        <option value="en">English</option>
        <option value="ja">日本語</option>
      </select>

      <style>{`
        .language-switcher {
          display: inline-block;
        }
        
        .language-select {
          padding: 6px 12px;
          border: 1px solid #ccc;
          border-radius: 4px;
          background-color: white;
          font-size: 14px;
          cursor: pointer;
          min-width: 100px;
        }
        
        .language-select:focus {
          outline: none;
          border-color: #007bff;
          box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
        }
        
        .language-select:hover {
          border-color: #007bff;
        }
      `}</style>
    </div>
  )
}

export default LanguageSwitcher